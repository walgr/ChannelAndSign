#pragma once
#include <jni.h>
#include <cstddef>
#include <cstdint>

bool register_method_dex(const uint8_t *dex, size_t size, const uint8_t *data, size_t dataSize);
bool install_class_restore(JNIEnv *env, int sdk);
bool copy_protected_dex(const uint8_t *current, size_t size, uint8_t *output);
