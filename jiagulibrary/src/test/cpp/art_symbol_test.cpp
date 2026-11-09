// Host regression: exact ELF symbol lookup and bounded parsing of malformed input.
#define ART_SYMBOL_HOST_TEST
#include "../../main/cpp/utils/art_symbol.cpp"
#include <cassert>
#include <fstream>
#include <iterator>
#include <iostream>

int main(int argc, char **argv) {
    assert(argc == 2);
    std::ifstream input(argv[1], std::ios::binary);
    Bytes b((std::istreambuf_iterator<char>(input)), std::istreambuf_iterator<char>());
    Ehdr e{};
    assert(header(b, e));
    Shdr names{};
    assert(section(b, e, e.e_shstrndx, names));
    bool found = false;
    for (size_t i = 0; i < e.e_shnum; ++i) {
        Shdr s{};
        if (!section(b, e, i, s) || !nameEquals(b, names, s.sh_name, ".gnu_debugdata")) continue;
        xz_crc32_init(); xz_crc64_init();
        Bytes out(4 * 1024 * 1024);
        xz_buf buffer{};
        buffer.in = b.data() + s.sh_offset; buffer.in_size = s.sh_size;
        buffer.out = out.data(); buffer.out_size = out.size();
        xz_dec *decoder = xz_dec_init(XZ_SINGLE, 0);
        assert(decoder);
        assert(xz_dec_run(decoder, &buffer) == XZ_STREAM_END);
        xz_dec_end(decoder);
        out.resize(buffer.out_pos);
        const char *name = "_ZN3art11ClassLinker11DefineClassEPNS_6ThreadEPKcmmNS_6HandleINS_6mirror11ClassLoaderEEERKNS_7DexFileERKNS_3dex8ClassDefE";
        assert(symbol(out, name) != 0);
        assert(symbol(out, "DefineClass") == 0);
        for (size_t size : {size_t(0), size_t(16), sizeof(Ehdr), out.size() / 2}) {
            Bytes truncated(out.begin(), out.begin() + size);
            assert(symbol(truncated, name) == 0);
        }
        Ehdr invalid{}; memcpy(&invalid, out.data(), sizeof(invalid));
        invalid.e_shoff = ~decltype(invalid.e_shoff)(0);
        memcpy(out.data(), &invalid, sizeof(invalid));
        assert(symbol(out, name) == 0);
        found = true;
    }
    assert(found);
    std::cout << "ART mini-debug decode, exact symbol and malformed ELF checks passed\n";
}
