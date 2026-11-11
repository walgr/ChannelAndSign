#pragma once

#include <cstdint>
#include <cstring>
#include <map>
#include <set>
#include <vector>
#include <array>

namespace method_restore {

inline uint32_t le32(const uint8_t *p) {
    return uint32_t(p[0]) | (uint32_t(p[1]) << 8) |
           (uint32_t(p[2]) << 16) | (uint32_t(p[3]) << 24);
}

inline uint32_t be32(const uint8_t *p) {
    return (uint32_t(p[0]) << 24) | (uint32_t(p[1]) << 16) |
           (uint32_t(p[2]) << 8) | uint32_t(p[3]);
}

inline bool range(size_t offset, size_t length, size_t size) {
    return offset <= size && length <= size - offset;
}

struct Dex {
    const uint8_t *source = nullptr;
    size_t size = 0;
    uint32_t classDefsOffset = 0;
    uint32_t classDefsCount = 0;
    std::map<uint32_t, std::vector<uint8_t>> instructions;
    std::map<uint32_t, std::vector<uint32_t>> classes;
    std::array<uint8_t, 112> header{};
    std::map<uint32_t, std::array<uint8_t, 32>> definitions;

    // Reopening ART's live mapping must not carry partially restored code with
    // the protected header checksum. Recreate the producer's zeroed code spans
    // in a separate buffer; never zero instructions that ART is executing.
    bool protectedCopy(const uint8_t *current, size_t currentSize, uint8_t *output) const {
        if (currentSize != size || std::memcmp(current, header.data(), header.size()) != 0) return false;
        std::memcpy(output, current, size);
        for (const auto &entry : instructions) std::memset(output + entry.first, 0, entry.second.size());
        return true;
    }

    bool uleb(size_t &cursor, uint32_t &value) const {
        value = 0;
        for (unsigned i = 0; i < 5; ++i) {
            if (cursor >= size) return false;
            const uint8_t b = source[cursor++];
            if (i == 4 && (b & 0xf0)) return false;
            value |= uint32_t(b & 0x7f) << (i * 7);
            if (!(b & 0x80)) return true;
        }
        return false;
    }

    // Wire format is unchanged: big-endian insns offset, byte length, bytes.
    bool parse(const uint8_t *dex, size_t dexSize, const uint8_t *data, size_t dataSize) {
        source = dex;
        size = dexSize;
        instructions.clear();
        classes.clear();
        definitions.clear();
        if (size < 112 || std::memcmp(source, "dex\n", 4) != 0 ||
            le32(source + 32) != size || le32(source + 40) != 0x12345678) return false;
        classDefsCount = le32(source + 96);
        classDefsOffset = le32(source + 100);
        if (classDefsCount > size / 32 ||
            !range(classDefsOffset, size_t(classDefsCount) * 32, size)) return false;
        std::memcpy(header.data(), source, header.size());
        for (size_t cursor = 0; cursor < dataSize;) {
            if (!range(cursor, 8, dataSize)) return false;
            uint32_t offset = be32(data + cursor), length = be32(data + cursor + 4);
            cursor += 8;
            if (offset < 16 || (offset & 1) || (length & 1) ||
                !range(offset, length, size) || !range(cursor, length, dataSize) ||
                le32(source + offset - 4) != length / 2 || instructions.count(offset)) return false;
            instructions[offset] = std::vector<uint8_t>(data + cursor, data + cursor + length);
            cursor += length;
        }
        if (instructions.empty()) return true;
        std::set<uint32_t> matched;
        for (uint32_t i = 0; i < classDefsCount; ++i) {
            const uint32_t defOffset = classDefsOffset + i * 32;
            size_t cursor = le32(source + defOffset + 24);
            if (cursor == 0) continue;
            uint32_t counts[4];
            for (auto &count : counts) if (!uleb(cursor, count)) return false;
            // Every field/method consumes at least two/three ULEB bytes.
            if (uint64_t(counts[0]) + counts[1] + counts[2] + counts[3] > size) return false;
            uint32_t ignored;
            for (uint64_t j = 0; j < uint64_t(counts[0]) + counts[1]; ++j) {
                if (!uleb(cursor, ignored) || !uleb(cursor, ignored)) return false;
            }
            for (uint64_t j = 0; j < uint64_t(counts[2]) + counts[3]; ++j) {
                uint32_t code;
                if (!uleb(cursor, ignored) || !uleb(cursor, ignored) || !uleb(cursor, code)) return false;
                if (!code) continue; // abstract/native methods
                if (!range(code, 16, size)) return false;
                const uint32_t offset = code + 16;
                if (instructions.count(offset)) {
                    classes[defOffset].push_back(offset);
                    std::memcpy(definitions[defOffset].data(), source + defOffset, 32);
                    matched.insert(offset);
                }
            }
        }
        return matched.size() == instructions.size();
    }
};

} // namespace method_restore
