# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

project("jiagu")

# ARM/Thumb instruction relocation is vendored and built statically; no extra SO to extract.
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(Plugin.SymbolResolver OFF CACHE BOOL "" FORCE)
    set(DOBBY_DEBUG OFF CACHE BOOL "" FORCE)
    set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
    set(NearBranch OFF CACHE BOOL "" FORCE)
    add_subdirectory(third_party/dobby EXCLUDE_FROM_ALL)
endif()

option(METHOD_RESTORE_TESTS "Build extracted method parser regression tests" OFF)
if(METHOD_RESTORE_TESTS)
    add_executable(aes_payload_test ../../test/cpp/aes_payload_test.c utils/aes.c)
    target_include_directories(aes_payload_test PRIVATE utils)
    target_compile_options(aes_payload_test PRIVATE -UNDEBUG)
    add_executable(method_restore_test ../../test/cpp/method_restore_test.cpp)
    target_include_directories(method_restore_test PRIVATE utils)
    target_compile_options(method_restore_test PRIVATE -UNDEBUG)
    if(ANDROID_ABI STREQUAL "x86" OR ANDROID_ABI STREQUAL "x86_64")
        add_executable(x86_hook_test ../../test/cpp/x86_hook_test.cpp
                utils/x86_hook.cpp utils/hde/hde32.c utils/hde/hde64.c)
        target_include_directories(x86_hook_test PRIVATE utils)
        target_link_libraries(x86_hook_test log)
        target_compile_options(x86_hook_test PRIVATE -UNDEBUG)
    endif()
    if(ANDROID_ABI STREQUAL "armeabi-v7a")
        add_executable(arm_hook_test ../../test/cpp/arm_hook_test.cpp)
        target_link_libraries(arm_hook_test dobby log)
        target_compile_options(arm_hook_test PRIVATE -UNDEBUG)
    endif()
endif()

add_library(
        jiagu
        SHARED
        jiagu.cpp
        utils/d2c_generated.cpp
        utils/class_restore.cpp
        utils/art_symbol.cpp
        utils/xz/xz_crc32.c
        utils/xz/xz_crc64.c
        utils/xz/xz_dec_stream.c
        utils/xz/xz_dec_lzma2.c
        utils/x86_hook.cpp
        utils/hde/hde32.c
        utils/hde/hde64.c
        utils/And64InlineHook.cpp
        utils/memload.cpp
        utils/aes.c
        utils/dlopen.c
        utils/invoke_java.cpp
)

include_directories(
        utils
)

target_link_libraries(
        jiagu
        log)
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_link_libraries(jiagu dobby)
endif()
