#include "art_symbol.h"
#ifndef ART_SYMBOL_HOST_TEST
#include "dlopen.h"
#else
#include <dlfcn.h>
#define ndk_dlsym dlsym
#endif
#include <elf.h>
#include <link.h>
#include <cstdio>
#include <cstring>
#include <vector>
#include <cstdint>
#include <mutex>
extern "C" {
#include "xz/xz.h"
void xz_crc64_init(void);
}

namespace {
using Bytes = std::vector<uint8_t>;
bool range(size_t offset, size_t size, size_t total) {
    return offset <= total && size <= total - offset;
}
template<class T> bool read(const Bytes &b, size_t offset, T &value) {
    if (!range(offset, sizeof(T), b.size())) return false;
    memcpy(&value, b.data() + offset, sizeof(T));
    return true;
}
using Ehdr = ElfW(Ehdr);
using Shdr = ElfW(Shdr);
using Sym = ElfW(Sym);
bool header(const Bytes &b, Ehdr &e) {
    return read(b, 0, e) && !memcmp(e.e_ident, ELFMAG, SELFMAG) &&
        e.e_ident[EI_CLASS] == (sizeof(void *) == 8 ? ELFCLASS64 : ELFCLASS32) &&
        e.e_ident[EI_DATA] == ELFDATA2LSB && e.e_shentsize == sizeof(Shdr) &&
        range(e.e_shoff, size_t(e.e_shnum) * sizeof(Shdr), b.size());
}
bool section(const Bytes &b, const Ehdr &e, size_t index, Shdr &s) {
    return index < e.e_shnum && read(b, e.e_shoff + index * sizeof(Shdr), s) &&
        range(s.sh_offset, s.sh_size, b.size());
}
bool nameEquals(const Bytes &b, const Shdr &s, size_t offset, const char *name) {
    size_t n = strlen(name) + 1;
    return range(offset, n, s.sh_size) && !memcmp(b.data() + s.sh_offset + offset, name, n);
}
uintptr_t symbol(const Bytes &b, const char *name) {
    Ehdr e{};
    if (!header(b, e)) return 0;
    for (size_t i = 0; i < e.e_shnum; ++i) {
        Shdr s{}, strings{};
        if (!section(b, e, i, s) || (s.sh_type != SHT_SYMTAB && s.sh_type != SHT_DYNSYM) ||
            s.sh_entsize != sizeof(Sym) || !section(b, e, s.sh_link, strings)) continue;
        for (size_t pos = 0; range(pos, sizeof(Sym), s.sh_size); pos += sizeof(Sym)) {
            Sym sym{};
            if (!read(b, s.sh_offset + pos, sym)) break;
            if ((sym.st_info & 15) == STT_FUNC && sym.st_shndx != SHN_UNDEF &&
                nameEquals(b, strings, sym.st_name, name)) return sym.st_value;
        }
    }
    return 0;
}
struct Loaded { const char *path; uintptr_t value; void *address; };
int loaded(dl_phdr_info *info, size_t, void *opaque) {
    auto &v = *static_cast<Loaded *>(opaque);
    if (strcmp(info->dlpi_name, v.path)) return 0;
    for (size_t i = 0; i < info->dlpi_phnum; ++i) {
        const auto &p = info->dlpi_phdr[i];
        if (p.p_type == PT_LOAD && (p.p_flags & PF_X) && v.value >= p.p_vaddr &&
            v.value - p.p_vaddr < p.p_memsz) {
            v.address = reinterpret_cast<void *>(info->dlpi_addr + v.value);
            return 1;
        }
    }
    return 0;
}
}
void *resolve_art_symbol(void *handle, const char *path, const char *name) {
    if (void *address = ndk_dlsym(handle, name)) return address;
    FILE *file = fopen(path, "rb");
    if (!file) return nullptr;
    fseek(file, 0, SEEK_END);
    long size = ftell(file);
    rewind(file);
    if (size <= 0 || size > 128 * 1024 * 1024) { fclose(file); return nullptr; }
    Bytes b(size);
    bool ok = fread(b.data(), 1, b.size(), file) == b.size();
    fclose(file);
    Ehdr e{};
    if (!ok || !header(b, e)) return nullptr;
    uintptr_t value = symbol(b, name);
    if (!value) {
        Shdr names{};
        if (!section(b, e, e.e_shstrndx, names)) return nullptr;
        for (size_t i = 0; i < e.e_shnum; ++i) {
            Shdr s{};
            if (!section(b, e, i, s) || !nameEquals(b, names, s.sh_name, ".gnu_debugdata")) continue;
            static std::once_flag crcInit;
            std::call_once(crcInit, [] { xz_crc32_init(); xz_crc64_init(); });
            for (size_t capacity = 1024 * 1024; capacity <= 64 * 1024 * 1024; capacity *= 2) {
                Bytes out(capacity);
                xz_dec *decoder = xz_dec_init(XZ_SINGLE, 0);
                if (!decoder) break;
                xz_buf buffer{};
                buffer.in = b.data() + s.sh_offset;
                buffer.in_size = s.sh_size;
                buffer.out = out.data();
                buffer.out_size = out.size();
                xz_ret result = xz_dec_run(decoder, &buffer);
                xz_dec_end(decoder);
                if (result == XZ_STREAM_END && buffer.in_pos == s.sh_size) {
                    out.resize(buffer.out_pos); value = symbol(out, name); break;
                }
                if (result != XZ_BUF_ERROR) break;
            }
            break;
        }
    }
    Loaded v{path, value, nullptr};
    if (value) dl_iterate_phdr(loaded, &v);
    return v.address;
}
