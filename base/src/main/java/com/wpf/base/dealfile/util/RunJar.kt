package com.wpf.base.dealfile.util

object RunJar {

    fun javaJar(jarFile: String, cmd: Array<String>) = arrayListOf("java", "-jar", jarFile).also {
        it.addAll(cmd)
    }.toTypedArray()
}