# HDE instruction decoder

Source: MinHook **v1.3.4**, https://github.com/TsudaKageyu/minhook/tree/v1.3.4/src/hde

Vendored files: hde32.c/h, hde64.c/h, table32.h, table64.h, pstdint.h.
`LICENSE.txt` is the upstream MinHook license including the HDE licenses.

Local change: pstdint.h includes the platform `<stdint.h>` instead of Windows.h
and Windows integer typedefs. Decoder logic and tables are unchanged.

The POSIX hook installer and relocator in ../x86_hook.cpp are local code and
use HDE to decode complete instructions. There is no network dependency during
shell builds. The license file travels with the shell-source ZIP.
