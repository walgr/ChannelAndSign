#include "method_restore.h"
#include <cassert>
#include <cstdio>

using method_restore::Dex;
using Bytes = std::vector<uint8_t>;

static void le(Bytes &b, size_t offset, uint32_t value) {
    for (int i = 0; i < 4; ++i) b[offset + i] = value >> (i * 8);
}

static void record(Bytes &data, uint32_t offset, std::initializer_list<uint8_t> code) {
    for (int shift = 24; shift >= 0; shift -= 8) data.push_back(offset >> shift);
    for (int shift = 24; shift >= 0; shift -= 8) data.push_back(code.size() >> shift);
    data.insert(data.end(), code);
}

// Two classes: a direct method in class 0, a virtual method and native method in class 1.
static Bytes fixture() {
    Bytes dex(288, 0);
    std::memcpy(dex.data(), "dex\n035\0", 8);
    le(dex, 32, dex.size());
    le(dex, 40, 0x12345678);
    le(dex, 96, 2);
    le(dex, 100, 112);
    le(dex, 112 + 24, 176);
    le(dex, 144 + 24, 184);
    const uint8_t direct[] = {0, 0, 1, 0, 0, 1, 0xd0, 1}; // code_off = 208
    const uint8_t virtuals[] = {0, 0, 0, 2, 0, 1, 0xf0, 1, 1, 1, 0}; // code_off = 240, 0
    std::memcpy(dex.data() + 176, direct, sizeof(direct));
    std::memcpy(dex.data() + 184, virtuals, sizeof(virtuals));
    le(dex, 208 + 12, 2);
    le(dex, 240 + 12, 1);
    return dex;
}

int main() {
    auto dex = fixture();
    Bytes records;
    record(records, 224, {0x12, 0x00, 0x0e, 0x00});
    record(records, 256, {0x0e, 0x00});
    Dex parsed;
    assert(parsed.parse(dex.data(), dex.size(), records.data(), records.size()));
    assert(parsed.classes.at(112) == std::vector<uint32_t>{224});
    assert(parsed.classes.at(144) == std::vector<uint32_t>{256});
    assert(dex[224] == 0 && dex[256] == 0); // Parsing must never restore eagerly.
    auto actual = dex; // ART can copy the source buffer.
    for (uint32_t offset : parsed.classes.at(112)) {
        auto &code = parsed.instructions.at(offset);
        std::memcpy(actual.data() + offset, code.data(), code.size());
    }
    assert(actual[224] == 0x12 && actual[256] == 0 && dex[224] == 0);
    // Identical offsets in another DEX retain distinct method data.
    Bytes otherRecords;
    record(otherRecords, 224, {0x13, 0x00, 0x0e, 0x00});
    Dex other;
    assert(other.parse(dex.data(), dex.size(), otherRecords.data(), otherRecords.size()));
    assert(other.instructions.at(224)[0] != parsed.instructions.at(224)[0]);
    assert(parsed.parse(dex.data(), dex.size(), nullptr, 0));
    auto truncated = records;
    truncated.pop_back();
    assert(!parsed.parse(dex.data(), dex.size(), truncated.data(), truncated.size()));
    Bytes invalid;
    record(invalid, 0xfffffff0, {0, 0});
    assert(!parsed.parse(dex.data(), dex.size(), invalid.data(), invalid.size()));
    invalid.clear();
    record(invalid, 224, {0, 0}); // insns_size mismatch
    assert(!parsed.parse(dex.data(), dex.size(), invalid.data(), invalid.size()));
    invalid = records;
    record(invalid, 224, {0, 0, 0, 0});
    assert(!parsed.parse(dex.data(), dex.size(), invalid.data(), invalid.size()));
    // Malformed and truncated ULEB128 must not read past the DEX.
    le(dex, 112 + 24, 287);
    dex[287] = 0x80;
    assert(!parsed.parse(dex.data(), dex.size(), nullptr, 0));
    std::puts("method_restore tests passed");
}
