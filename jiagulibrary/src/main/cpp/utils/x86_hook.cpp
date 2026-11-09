#include "x86_hook.h"

#if defined(__i386__) || defined(__x86_64__)
#include <cstdint>
#include <cstring>
#include <climits>
#include <initializer_list>
#include <sys/mman.h>
#include <unistd.h>

#if defined(__x86_64__)
#include "hde/hde64.h"
using Instruction = hde64s;
#define DECODE hde64_disasm
constexpr size_t jumpSize = 14;
constexpr size_t callSize = 16;
constexpr size_t conditionalSize = 16;
#else
#include "hde/hde32.h"
using Instruction = hde32s;
#define DECODE hde32_disasm
constexpr size_t jumpSize = 5;
constexpr size_t callSize = 5;
constexpr size_t conditionalSize = 6;
#endif

namespace {
enum class Kind { Copy, Call, Jump, Conditional, PcThunk, PushPc };
struct Relocated {
    Instruction decoded;
    size_t from, to, length;
    uintptr_t destination = 0;
    Kind kind = Kind::Copy;
    uint8_t reg = 0;
};

bool relative32(uintptr_t from, uintptr_t to, int32_t &result) {
#if defined(__x86_64__)
    if (to >= from ? to - from > INT32_MAX : from - to > uint64_t(INT32_MAX) + 1) return false;
#endif
    result = static_cast<int32_t>(to - from);
    return true;
}

void jump(uint8_t *out, uintptr_t destination) {
#if defined(__x86_64__)
    const uint8_t prefix[] = {0xff, 0x25, 0, 0, 0, 0};
    std::memcpy(out, prefix, sizeof(prefix));
    std::memcpy(out + 6, &destination, 8);
#else
    out[0] = 0xe9;
    int32_t rel = static_cast<int32_t>(destination - reinterpret_cast<uintptr_t>(out + 5));
    std::memcpy(out + 1, &rel, 4);
#endif
}

void *allocateNear(uintptr_t target, size_t size) {
#if defined(__x86_64__)
    // Hints never replace existing mappings. A nearby trampoline is required for
    // RIP-relative instructions and the five-byte jump to its detour relay.
    for (uintptr_t distance = 0x10000; distance < 0x70000000; distance += 0x1000000) {
        for (int sign : {1, -1}) {
            if (sign < 0 && target < distance) continue;
            uintptr_t hint = sign > 0 ? target + distance : target - distance;
            hint -= hint % size;
            void *memory = mmap(reinterpret_cast<void *>(hint), size, PROT_READ | PROT_WRITE,
                                MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
            if (memory == MAP_FAILED) continue;
            int32_t ignored;
            if (relative32(target + 5, reinterpret_cast<uintptr_t>(memory) + size - 16, ignored)) return memory;
            munmap(memory, size);
        }
    }
    return MAP_FAILED;
#else
    (void)target;
    return mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
#endif
}

bool buildTrampoline(const uint8_t *source, uint8_t *output, size_t &consumed) {
    Relocated instructions[8];
    size_t count = 0, produced = 0;
    consumed = 0;
    while (consumed < 5) {
        auto &item = instructions[count++];
        item.from = consumed;
        item.to = produced;
        const uint8_t *input = source + consumed;
        auto &h = item.decoded;
        DECODE(input, &h);
        if (!h.len || (h.flags & F_ERROR) || h.opcode == 0xc4 || h.opcode == 0xc5 || h.opcode == 0x62) return false;
        item.length = h.len;
        // Far transfers, loop/JCXZ and premature returns cannot be safely moved.
        if (h.opcode == 0xea || h.opcode == 0x9a || (h.opcode >= 0xe0 && h.opcode <= 0xe3) ||
            h.opcode == 0xc2 || h.opcode == 0xc3 || h.opcode == 0xca || h.opcode == 0xcb) return false;
        if (h.flags & F_RELATIVE) {
            if (h.p_66) return false;
            int32_t displacement = (h.flags & F_IMM8) ? int8_t(h.imm.imm8) : int32_t(h.imm.imm32);
            item.destination = reinterpret_cast<uintptr_t>(input + h.len) + displacement;
            if (h.opcode == 0xe8) {
                item.kind = Kind::Call;
                item.length = callSize;
#if defined(__i386__)
                // GCC/Clang PIC: get_pc_thunk.<reg> must return the ORIGINAL PC.
                const auto *callee = reinterpret_cast<const uint8_t *>(item.destination);
                if (callee[0] == 0x8b && (callee[1] & 0xc7) == 0x04 && callee[2] == 0x24 && callee[3] == 0xc3) {
                    item.kind = Kind::PcThunk;
                    item.reg = (callee[1] >> 3) & 7;
                } else if (displacement == 0) {
                    item.kind = Kind::PushPc; // call next; pop <reg>
                }
#endif
            } else if (h.opcode == 0xe9 || h.opcode == 0xeb) {
                item.kind = Kind::Jump;
                item.length = jumpSize;
            } else if ((h.opcode & 0xf0) == 0x70 || (h.opcode == 0x0f && (h.opcode2 & 0xf0) == 0x80)) {
                item.kind = Kind::Conditional;
                item.length = conditionalSize;
            } else return false;
        }
        consumed += h.len;
        produced += item.length;
    }
    for (size_t i = 0; i < count; ++i) {
        const auto &item = instructions[i];
        const auto &h = item.decoded;
        uint8_t *out = output + item.to;
        uintptr_t destination = item.destination;
        uintptr_t sourceAddress = reinterpret_cast<uintptr_t>(source);
        if (destination >= sourceAddress && destination < sourceAddress + consumed &&
            item.kind != Kind::PcThunk && item.kind != Kind::PushPc) {
            bool found = false;
            for (size_t j = 0; j < count; ++j) {
                if (instructions[j].from == destination - sourceAddress) {
                    destination = reinterpret_cast<uintptr_t>(output + instructions[j].to);
                    found = true;
                    break;
                }
            }
            if (!found) return false; // Jump into the middle of an instruction.
        }
        switch (item.kind) {
            case Kind::Copy: {
                std::memcpy(out, source + item.from, h.len);
#if defined(__x86_64__)
                if ((h.flags & F_MODRM) && h.modrm_mod == 0 && h.modrm_rm == 5) {
                    if (h.p_67) return false;
                    size_t immediate = ((h.flags & F_IMM8) ? 1 : 0) + ((h.flags & F_IMM16) ? 2 : 0) +
                                       ((h.flags & F_IMM32) ? 4 : 0) + ((h.flags & F_IMM64) ? 8 : 0);
                    if (h.len < immediate + 4) return false;
                    uintptr_t absolute = sourceAddress + item.from + h.len + int32_t(h.disp.disp32);
                    int32_t displacement;
                    if (!relative32(reinterpret_cast<uintptr_t>(out + h.len), absolute, displacement)) return false;
                    std::memcpy(out + h.len - immediate - 4, &displacement, 4);
                }
#endif
                break;
            }
            case Kind::Jump:
                jump(out, destination);
                break;
            case Kind::Call:
#if defined(__x86_64__)
                // call [rip+2]; jmp +8; <absolute target>. Preserve every register.
                std::memcpy(out, "\xff\x15\x02\x00\x00\x00\xeb\x08", 8);
                std::memcpy(out + 8, &destination, 8);
#else
                out[0] = 0xe8;
                { int32_t rel = static_cast<int32_t>(destination - reinterpret_cast<uintptr_t>(out + 5));
                  std::memcpy(out + 1, &rel, 4); }
#endif
                break;
            case Kind::Conditional: {
                uint8_t condition = (h.opcode == 0x0f ? h.opcode2 : h.opcode) & 15;
#if defined(__x86_64__)
                out[0] = 0x70 | (condition ^ 1);
                out[1] = 14;
                jump(out + 2, destination);
#else
                out[0] = 0x0f;
                out[1] = 0x80 | condition;
                int32_t rel = static_cast<int32_t>(destination - reinterpret_cast<uintptr_t>(out + 6));
                std::memcpy(out + 2, &rel, 4);
#endif
                break;
            }
            case Kind::PcThunk:
            case Kind::PushPc: {
                out[0] = item.kind == Kind::PcThunk ? 0xb8 + item.reg : 0x68;
                uint32_t pc = static_cast<uint32_t>(sourceAddress + item.from + h.len);
                std::memcpy(out + 1, &pc, 4);
                break;
            }
        }
    }
    jump(output + produced, reinterpret_cast<uintptr_t>(source + consumed));
    return true;
}
} // namespace

bool install_x86_hook(void *target, void *replacement, void **original) {
    if (!target || !replacement || !original || *original) return false;
    long pageSize = sysconf(_SC_PAGESIZE);
    if (pageSize <= 0) return false;
    auto *trampoline = static_cast<uint8_t *>(allocateNear(reinterpret_cast<uintptr_t>(target), pageSize));
    if (trampoline == MAP_FAILED) return false;
    size_t consumed;
    if (!buildTrampoline(static_cast<const uint8_t *>(target), trampoline, consumed)) {
        munmap(trampoline, pageSize);
        return false;
    }
    auto *relay = trampoline + pageSize - 16;
    jump(relay, reinterpret_cast<uintptr_t>(replacement));
    int32_t displacement;
    if (!relative32(reinterpret_cast<uintptr_t>(target) + 5, reinterpret_cast<uintptr_t>(relay), displacement) ||
        mprotect(trampoline, pageSize, PROT_READ | PROT_EXEC) != 0) {
        munmap(trampoline, pageSize);
        return false;
    }
    uintptr_t start = reinterpret_cast<uintptr_t>(target);
    uintptr_t page = start - start % pageSize;
    size_t length = start - page + consumed;
    if (mprotect(reinterpret_cast<void *>(page), length, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        munmap(trampoline, pageSize);
        return false;
    }
    // Publish the callable original before the detour becomes reachable.
    *original = trampoline;
    auto *patch = static_cast<uint8_t *>(target);
    patch[0] = 0xe9;
    std::memcpy(patch + 1, &displacement, 4);
    std::memset(patch + 5, 0x90, consumed - 5);
    __builtin___clear_cache(reinterpret_cast<char *>(target), reinterpret_cast<char *>(target) + consumed);
    // ART code pages were RX. The trampoline remains RX for the process lifetime.
    mprotect(reinterpret_cast<void *>(page), length, PROT_READ | PROT_EXEC);
    return true;
}
#else
bool install_x86_hook(void *, void *, void **) { return false; }
#endif
