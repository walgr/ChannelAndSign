# Dobby ARM/Thumb dependency

Source: https://github.com/jmpews/Dobby
Pinned commit: 9c85e74f92eda36b99ddb0fddb17a9df2278a4d3
License: Apache-2.0 (see LICENSE and retain per-file notices).

Vendored build/source directories only. Only armeabi-v7a uses static dobby;
other ABIs retain existing hooks. Symbol resolver, debug logging and near
branch plugins are disabled by the parent CMake. No build-time network.

Local change: ARM LSL carry extraction avoids undefined 32-bit shifts.
Local change: Thumb-2 literal loads use Align(PC,4), including instructions
starting at addresses congruent to 2 mod 4 (the API 21 ART DefineClass prologue).
