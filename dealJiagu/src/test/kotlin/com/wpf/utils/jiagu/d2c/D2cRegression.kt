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
        "overloaded(I)I", "overloaded(II)I", "under_score(I)I", "bitwise(II)I", "compare(II)I", "zeroCompare(I)I").map { prefix + it }.toSet()
    val input = mapOf("classes.dex" to original)
    val defaults = D2cCompiler.selectDefaults(input, "com.wpf.jiagu.d2c")
    val narrowPrefix = "Lcom/wpf/jiagu/d2c/NarrowSmoke;->"
    val narrow = listOf("asByte(I)B", "asShort(I)S", "asChar(I)C", "mix(BSC)I", "choose(ZZ)Z", "narrowLoop(I)S", "conversions(I)I")
        .map { narrowPrefix + it }.toSet()
    check(defaults.methods == selected + (prefix + "mainProcess(I)I") + narrow)
    listOf("narrow-d2c" to D2cCompiler.compile(input, narrow),
        "narrow-hybrid" to D2cCompiler.compileHybrid(input, narrow),
        "narrow-vmp" to D2cCompiler.compile(input, narrow, narrow)).forEach { (name, result) ->
        val directory = File(output.parentFile, name).apply { mkdirs() }
        File(directory, "classes.dex").writeBytes(result.dexFiles.getValue("classes.dex"))
        File(directory, "d2c_generated.cpp").writeText(result.source)
        check(result.methods.toSet() == narrow)
    }
    check(prefix + "unsupported(I)I" in defaults.skipped)
    check(prefix + "catchBlock(I)I" in defaults.skipped)
    check(D2cCompiler.selectDefaults(input, "com.wpf.jiagu").methods == defaults.methods)
    check(D2cCompiler.selectDefaults(input, "com.wpf.jiagu.d2").methods.isEmpty())
    check(D2cCompiler.selectDefaults(input, "com.wpf.jiagu.d2c2").methods.isEmpty())
    check(runCatching { D2cCompiler.selectDefaults(input, "") }.isFailure)
    val auto = D2cCompiler.compile(input, defaults.methods)
    val autoMethods = DexBackedDexFile(null, auto.dexFiles.getValue("classes.dex")).classes.flatMap { it.methods }
    check(autoMethods.count { AccessFlags.NATIVE.isSet(it.accessFlags) } == defaults.methods.size)
    val hybrid = D2cCompiler.compileHybrid(input, selected)
    check(setOf(prefix + "mix(II)I", prefix + "loop(I)I", prefix + "compare(II)I", prefix + "zeroCompare(I)I")
        .all { it in hybrid.vmpMethods })
    check(prefix + "divide(II)I" in hybrid.d2cMethods)
    check(hybrid.vmpMethods.toSet().intersect(hybrid.d2cMethods.toSet()).isEmpty())
    check(hybrid.vmpMethods.size + hybrid.d2cMethods.size == selected.size)
    val virtualized = D2cCompiler.compile(input, selected, selected)
    check(virtualized.vmpMethods.toSet() == selected && virtualized.d2cMethods.isEmpty())
    check(runCatching { D2cCompiler.compile(input, selected, setOf(prefix + "missing(I)I")) }.isFailure)
    listOf("hybrid" to hybrid, "virtualized" to virtualized).forEach { (name, compiled) ->
        val directory = File(output.parentFile, name).apply { mkdirs() }
        File(directory, "classes.dex").writeBytes(compiled.dexFiles.getValue("classes.dex"))
        File(directory, "d2c_generated.cpp").writeText(compiled.source)
        File(directory, "protection.txt").writeText(compiled.methods.joinToString("\n") {
            "${if (it in compiled.vmpMethods) "VMP" else "D2C"} $it"
        })
    }
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
    config.writeText(narrow.joinToString("\n"))
    check(D2cRules.read(config) == narrow)
    config.writeText("Lcom/example/*;->*(I)I")
    // Wildcards must never broaden the protected method set.
    check(runCatching { D2cRules.read(config) }.isFailure)
    config.delete()
    println("D2C compiler regression passed: ${selected.size} methods, native declarations, checksums, fail-closed rules")
}
