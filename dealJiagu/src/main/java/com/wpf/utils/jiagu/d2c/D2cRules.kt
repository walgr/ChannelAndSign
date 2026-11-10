package com.wpf.utils.jiagu.d2c

import java.io.File

object D2cRules {
    /** Exact DEX descriptors, one per line; no package substring or wildcard selection. */
    fun read(file: File): Set<String> {
        val methods = linkedSetOf<String>()
        file.readLines(Charsets.UTF_8).forEachIndexed { index, raw ->
            val line = raw.trim().removePrefix("\uFEFF")
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            require('*' !in line && '?' !in line && line.matches(Regex("L[^;\\s]+;->[^()\\s]+\\([IZBCS]*\\)[IZBCS]"))) {
                "D2C ${file.name}:${index + 1}: expected exact method descriptor with int/boolean/byte/char/short scalar types"
            }
            require(methods.add(line)) { "D2C duplicate rule at line ${index + 1}" }
        }
        require(methods.isNotEmpty()) { "D2C config is empty: ${file.name}" }
        require(methods.size <= 256) { "D2C supports at most 256 explicitly selected methods per APK" }
        return methods
    }
}
