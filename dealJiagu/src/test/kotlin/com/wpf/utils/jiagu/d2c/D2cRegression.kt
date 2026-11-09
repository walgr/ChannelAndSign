package com.wpf.utils.jiagu.d2c

import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import java.io.File
import java.security.MessageDigest
import java.util.zip.Adler32

fun main(args: Array<String>) {
    val original = File(args[0]).readBytes()
    val output = File(args[1]).apply { mkdirs() }
    val prefix = "Lcom/wpf/jiagu/d2c/D2cSmoke;->"
    val selected = listOf("mix(II)I", "divide(II)I", "remainder(II)I", "literal(I)I", "loop(I)I",
        "overloaded(I)I", "overloaded(II)I", "under_score(I)I").map { prefix + it }.toSet()
    val input = mapOf("classes.dex" to original)
    val defaults = D2cCompiler.selectDefaults(input, "com.wpf.jiagu.d2c")
    check(defaults.methods == selected + (prefix + "mainProcess(I)I"))
    check(prefix + "unsupported(I)I" in defaults.skipped)
    check(prefix + "catchBlock(I)I" in defaults.skipped)
    check(D2cCompiler.selectDefaults(input, "com.wpf.jiagu").methods == defaults.methods)
    check(D2cCompiler.selectDefaults(input, "com.wpf.jiagu.d2").methods.isEmpty())
    check(D2cCompiler.selectDefaults(input, "com.wpf.jiagu.d2c2").methods.isEmpty())
    check(runCatching { D2cCompiler.selectDefaults(input, "") }.isFailure)
    val auto = D2cCompiler.compile(input, defaults.methods)
    val autoMethods = DexBackedDexFile(null, auto.dexFiles.getValue("classes.dex")).classes.flatMap { it.methods }
    check(autoMethods.count { AccessFlags.NATIVE.isSet(it.accessFlags) } == defaults.methods.size)
    val result = D2cCompiler.compile(input, selected)
    val bytes = result.dexFiles.getValue("classes.dex")
    File(output, "classes.dex").writeBytes(bytes)
    val methods = DexBackedDexFile(null, bytes).classes.flatMap { it.methods }.associateBy(D2cCompiler::methodId)
    selected.forEach { id ->
        val method = methods.getValue(id)
        check(AccessFlags.NATIVE.isSet(method.accessFlags) && method.implementation == null) {
            "$id flags=${method.accessFlags} implementation=${method.implementation}"
        }
    }
    check(methods.getValue(prefix + "mainProcess(I)I").implementation != null)
    check(D2cCompiler.compile(input, emptySet()).dexFiles.getValue("classes.dex") === original)
    fun reject(rules: Set<String>) { check(runCatching { D2cCompiler.compile(input, rules) }.isFailure) }
    reject(setOf(prefix + "unsupported(I)I"))
    reject(setOf(prefix + "catchBlock(I)I"))
    reject(setOf(prefix + "missing(I)I"))
    check(D2cCompiler.jniName("a_b/\u4e2d$") == "a_1b__04e2d_00024")
    val sha = MessageDigest.getInstance("SHA-1").digest(bytes.copyOfRange(32, bytes.size))
    check(sha.contentEquals(bytes.copyOfRange(12, 32)))
    val checksum = Adler32().apply { update(bytes, 12, bytes.size - 12) }.value
    val expected = (0..3).fold(0L) { value, i -> value or ((bytes[8 + i].toLong() and 255) shl (8 * i)) }
    check(checksum == expected)
    File(output, "classes.dex").writeBytes(bytes)
    File(output, "d2c_generated.cpp").writeText(result.source)
    File(output, "methods.txt").writeText(result.methods.joinToString("\n"))
    val config = File(output, "rules.txt")
    config.writeText("# selected methods\n" + selected.joinToString("\n"))
    check(D2cRules.read(config) == selected)
    config.writeText("Lcom/example/*;->*(I)I")
    // Wildcards must never broaden the protected method set.
    check(runCatching { D2cRules.read(config) }.isFailure)
    config.delete()
    println("D2C compiler regression passed: ${selected.size} methods, native declarations, checksums, fail-closed rules")
}
