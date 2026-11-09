#include "x86_hook.h"
#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <initializer_list>
#include <sys/mman.h>
#include <unistd.h>

using Function = int (*)();
static int replacement() { return 42; }

static uint8_t *fixture(std::initializer_list<uint8_t> instructions) {
    auto *code = static_cast<uint8_t *>(mmap(nullptr, sysconf(_SC_PAGESIZE),
            PROT_READ | PROT_WRITE | PROT_EXEC, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0));
    assert(code != MAP_FAILED);
    std::memcpy(code, instructions.begin(), instructions.size());
    return code;
}

static void check(uint8_t *code, int expected) {
    auto function = reinterpret_cast<Function>(code);
    assert(function() == expected);
    void *original = nullptr;
    assert(install_x86_hook(code, reinterpret_cast<void *>(replacement), &original));
    assert(original != nullptr);
    assert(function() == 42);
    assert(reinterpret_cast<Function>(original)() == expected);
    // An already populated original slot must never install the hook twice.
    assert(!install_x86_hook(code, reinterpret_cast<void *>(replacement), &original));
}

int main() {
    check(fixture({0xb8, 7, 0, 0, 0, 0xc3}), 7);
    auto *call = fixture({0xe8, 59, 0, 0, 0, 0x83, 0xc0, 1, 0xc3});
    std::memcpy(call + 64, "\xb8\x0b\x00\x00\x00\xc3", 6);
    check(call, 12);
    // Taken conditional branch outside the relocated block.
    check(fixture({0x31, 0xc0, 0x74, 6, 0xb8, 1, 0, 0, 0, 0xc3,
                   0xb8, 2, 0, 0, 0, 0xc3}), 2);
    check(fixture({0x31, 0xc0, 0x0f, 0x84, 6, 0, 0, 0, 0xb8, 3, 0, 0, 0, 0xc3,
                   0xb8, 4, 0, 0, 0, 0xc3}), 4);
    auto *externalJump = fixture({0xe9, 59, 0, 0, 0});
    std::memcpy(externalJump + 64, "\xb8\x17\x00\x00\x00\xc3", 6);
    check(externalJump, 23);
    // Internal branch must follow the relocated instruction boundary.
    check(fixture({0xeb, 2, 0x90, 0x90, 0xb8, 7, 0, 0, 0, 0xc3}), 7);
#if defined(__x86_64__)
    auto *rip = fixture({0x8b, 0x05, 58, 0, 0, 0, 0xc3});
    rip[64] = 19;
    check(rip, 19);
    auto *ripImmediate = fixture({0x83, 0x3d, 57, 0, 0, 0, 1,
                                  0x0f, 0x94, 0xc0, 0x0f, 0xb6, 0xc0, 0xc3});
    ripImmediate[64] = 1;
    check(ripImmediate, 1);
#else
    auto *thunk = fixture({0x53, 0xe8, 58, 0, 0, 0, 0x89, 0xd8, 0x5b, 0xc3});
    std::memcpy(thunk + 64, "\x8b\x1c\x24\xc3", 4);
    check(thunk, static_cast<int>(reinterpret_cast<uintptr_t>(thunk + 6)));
    auto *callPop = fixture({0xe8, 0, 0, 0, 0, 0x58, 0xc3});
    check(callPop, static_cast<int>(reinterpret_cast<uintptr_t>(callPop + 5)));
#endif
    // Unsupported control flow must fail without changing executable bytes.
    auto *unsupported = fixture({0xe2, 0xfe, 0x90, 0x90, 0x90, 0xc3});
    uint8_t saved[6];
    std::memcpy(saved, unsupported, sizeof(saved));
    void *original = nullptr;
    assert(!install_x86_hook(unsupported, reinterpret_cast<void *>(replacement), &original));
    assert(!original && std::memcmp(saved, unsupported, sizeof(saved)) == 0);
    assert(!install_x86_hook(nullptr, reinterpret_cast<void *>(replacement), &original));
    std::puts("x86 hook tests passed");
}
