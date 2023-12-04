package com.wpf.base.dealfile.util

import java.io.File

fun File.createCheck(isFile: Boolean = false): File {
    if (!exists()) {
        if (parentFile?.exists() != true) {
            parentFile?.mkdirs()
        }
        if (isFile) {
            if (!exists()) {
                createNewFile()
            }
        } else {
            mkdir()
        }
    }
    return this
}