package com.wpf.utils.jiagu.d2c

import com.wpf.utils.jiagu.utils.AES128Helper
import com.wpf.utils.jiagu.utils.DexHelper
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

/** Integration fixture for an actual shell APK, using the same compiler/extraction/payload pipeline. */
fun main(args: Array<String>) {
    val inputs = mapOf("classes.dex" to File(args[0]).readBytes())
    val selected = D2cCompiler.selectDefaults(inputs, "com.wpf.jiagu.d2c")
    val hybrid = D2cCompiler.compileHybrid(inputs, selected.methods)
    check(hybrid.vmpMethods.isNotEmpty() && hybrid.d2cMethods.isNotEmpty())
    val dex = hybrid.dexFiles.getValue("classes.dex")
    val extracted = DexHelper.dealAllFunctionInNopInDex(dex, whiteList = arrayOf("com/wpf/jiagu/d2c"))
    check(extracted.isNotEmpty())
    val payload = ByteArrayOutputStream()
    DataOutputStream(payload).use { out ->
        val application = "com.wpf.jiagu.d2c.SmokeApp".toByteArray(Charsets.UTF_8)
        out.writeByte(application.size); out.write(application)
        out.writeInt(dex.size); out.write(dex)
        out.writeInt(extracted.size); out.write(extracted)
    }
    val encrypted = AES128Helper.encrypt(payload.toByteArray(), 512,
        secretKey = "0123456789abcdef", keyIv = "fedcba9876543210")
    val merged = DexHelper.mergeDex(File(args[1]).readBytes(), encrypted)
    val output = File(args[2]).apply { mkdirs() }
    File(output, "classes.dex").writeBytes(merged)
    File(output, "d2c_generated.cpp").writeText(hybrid.source)
    File(output, "protection.txt").writeText(hybrid.methods.joinToString("\n") {
        "${if (it in hybrid.vmpMethods) "VMP" else "D2C"} $it"
    })
    println("Hybrid payload fixture: D2C=${hybrid.d2cMethods.size} VMP=${hybrid.vmpMethods.size} extractedBytes=${extracted.size}")
}
