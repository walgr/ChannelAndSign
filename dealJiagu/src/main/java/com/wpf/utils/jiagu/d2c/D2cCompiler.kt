package com.wpf.utils.jiagu.d2c

import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.*
import org.jf.dexlib2.immutable.ImmutableClassDef
import org.jf.dexlib2.immutable.ImmutableDexFile
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodParameter
import org.jf.dexlib2.writer.io.MemoryDataStore
import org.jf.dexlib2.writer.pool.DexPool

/** D2C for pure static int calculations. Explicit unsupported selections fail before packaging. */
object D2cCompiler {
    internal fun jniType(type: String): String = when (type) {
        "Z" -> "jboolean"; "B" -> "jbyte"; "C" -> "jchar"; "S" -> "jshort"; "I" -> "jint"
        else -> error("Unsupported JNI scalar type $type")
    }

    internal fun returnValue(type: String, value: String): String = when (type) {
        "Z" -> "($value != 0 ? JNI_TRUE : JNI_FALSE)"
        "B" -> "jbyte(d2c_byte(uint32_t($value)))"
        "S" -> "jshort(d2c_short(uint32_t($value)))"
        "C" -> "jchar(uint32_t($value) & 65535u)"
        else -> value
    }
    data class Result(val dexFiles: Map<String, ByteArray>, val source: String, val methods: List<String>,
                      val vmpMethods: List<String> = emptyList()) {
        val d2cMethods: List<String> get() = methods - vmpMethods.toSet()
    }
    data class DefaultSelection(val methods: Set<String>, val skipped: Map<String, String>)
    private class UnsupportedMethod(message: String) : IllegalArgumentException(message)

    /** Include the manifest package and its subpackages, never a similarly named sibling package. */
    fun selectDefaults(inputs: Map<String, ByteArray>, packageName: String): DefaultSelection {
        require(packageName.isNotBlank() && '/' !in packageName && ';' !in packageName) {
            "D2C requires a valid APK package name for default selection"
        }
        val prefix = "L${packageName.replace('.', '/')}/"
        val selected = linkedSetOf<String>()
        val skipped = linkedMapOf<String, String>()
        inputs.values.forEach { bytes ->
            DexBackedDexFile(null, bytes).classes.filter { it.type.startsWith(prefix) }.forEach { cls ->
                cls.methods.forEach { method ->
                    val id = methodId(method)
                    try {
                        translate(method)
                        require(selected.add(id)) { "D2C duplicate method: $id" }
                    } catch (e: UnsupportedMethod) {
                        skipped[id] = e.message.orEmpty()
                    }
                }
            }
        }
        return DefaultSelection(selected, skipped)
    }

    fun methodId(method: Method): String =
        "${method.definingClass}->${method.name}(${method.parameterTypes.joinToString("")})${method.returnType}"

    /** Branching/looping or longer calculations use VMP; short straight-line code uses D2C. */
    fun compileHybrid(inputs: Map<String, ByteArray>, selected: Set<String>): Result {
        val vmp = inputs.values.flatMap { bytes -> DexBackedDexFile(null, bytes).classes }
            .flatMap { it.methods }.filter { methodId(it) in selected }
            .filter { method -> method.implementation?.instructions?.toList()?.let { instructions ->
                instructions.size >= 24 || instructions.any { it is OffsetInstruction }
            } == true }.map(::methodId).toSet()
        return compile(inputs, selected, vmp)
    }

    fun compile(inputs: Map<String, ByteArray>, selected: Set<String>, virtualized: Set<String> = emptySet()): Result {
        require(selected.containsAll(virtualized)) { "VMP methods must belong to the selected whitelist" }
        if (selected.isEmpty()) return Result(inputs, "", emptyList())
        val found = mutableSetOf<String>()
        val sources = mutableListOf<String>()
        val outputs = inputs.mapValues { (_, bytes) ->
            val dex = DexBackedDexFile(null, bytes)
            var changed = false
            val classes = dex.classes.map { cls ->
                val methods = cls.methods.map { method ->
                    val id = methodId(method)
                    if (id !in selected) method else {
                        require(found.add(id)) { "D2C duplicate method: $id" }
                        val nativeSource = translate(method) // Shared support/operand validation for both backends.
                        sources += if (id in virtualized) VmpCompiler.translate(method) else nativeSource
                        changed = true
                        // Parameter names belong to debug_info/code_item. Keeping them on a native
                        // method makes dexlib2 synthesize an invalid empty code_item for debug builds.
                        val nativeParameters = method.parameters.map { ImmutableMethodParameter(it.type, it.annotations, null) }
                        ImmutableMethod(method.definingClass, method.name, nativeParameters,
                            method.returnType, method.accessFlags or AccessFlags.NATIVE.value,
                            method.annotations, method.hiddenApiRestrictions, null)
                    }
                }
                ImmutableClassDef(cls.type, cls.accessFlags, cls.superclass, cls.interfaces,
                    cls.sourceFile, cls.annotations, cls.fields, methods)
            }
            if (!changed) bytes else {
                val store = MemoryDataStore()
                try {
                    DexPool.writeTo(store, ImmutableDexFile(dex.opcodes, classes))
                    store.data
                } finally { store.close() }
            }
        }
        require(found == selected) { "D2C methods not found: ${(selected - found).sorted().joinToString()}" }
        return Result(outputs, PREAMBLE + (if (virtualized.isEmpty()) "" else VmpCompiler.runtime) +
            sources.joinToString("\n"), found.sorted(), virtualized.sorted())
    }

    // JNI long names disambiguate overloads. Escape UTF-16 code units per JNI naming rules.
    internal fun jniName(value: String): String = buildString {
        value.forEach { c ->
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' -> append(c)
                c == '/' -> append('_')
                c == '_' -> append("_1")
                c == ';' -> append("_2")
                c == '[' -> append("_3")
                else -> append("_0%04x".format(c.code))
            }
        }
    }

    private fun translate(method: Method): String {
        val id = methodId(method)
        fun unsupported(reason: String): Nothing = throw UnsupportedMethod("D2C $id: $reason")
        if (!AccessFlags.STATIC.isSet(method.accessFlags) || AccessFlags.NATIVE.isSet(method.accessFlags) ||
            AccessFlags.ABSTRACT.isSet(method.accessFlags) || AccessFlags.SYNCHRONIZED.isSet(method.accessFlags) ||
            AccessFlags.DECLARED_SYNCHRONIZED.isSet(method.accessFlags)) unsupported("only non-synchronized static DEX methods are supported")
        val scalars = setOf("I", "Z", "B", "C", "S")
        if (method.returnType !in scalars || method.parameterTypes.any { it.toString() !in scalars })
            unsupported("only static int/boolean/byte/char/short parameters and return types are supported")
        val impl = method.implementation ?: unsupported("missing implementation")
        if (impl.tryBlocks.isNotEmpty()) unsupported("try/catch is not supported")
        val instructions = impl.instructions.toList()
        val addresses = mutableSetOf<Int>()
        var cursor = 0
        val at = instructions.map { instruction ->
            val pos = cursor
            addresses += pos
            cursor += instruction.codeUnits
            pos to instruction
        }
        if (at.isEmpty() || impl.registerCount < method.parameters.size) unsupported("invalid registers or empty code")
        fun reg(index: Int): String {
            if (index !in 0 until impl.registerCount) unsupported("invalid register v$index")
            return "r[$index]"
        }
        fun target(pos: Int, instruction: Instruction): String {
            val dest = pos + (instruction as OffsetInstruction).codeOffset
            if (dest !in addresses) unsupported("branch target is not an instruction")
            return "L$dest"
        }
        fun arithmetic(op: String, a: String, b: String): String = when (op) {
            "ADD" -> "$a + $b"
            "SUB" -> "$a - $b"
            "MUL" -> "$a * $b"
            "AND" -> "$a & $b"
            "OR" -> "$a | $b"
            "XOR" -> "$a ^ $b"
            "SHL" -> "$a << ($b & 31u)"
            "SHR" -> "d2c_sar($a, $b)"
            "USHR" -> "$a >> ($b & 31u)"
            else -> unsupported("unsupported arithmetic $op")
        }
        val body = at.joinToString("\n") { (pos, instruction) ->
            val op = instruction.opcode.toString()
            val one = instruction as? OneRegisterInstruction
            val two = instruction as? TwoRegisterInstruction
            val three = instruction as? ThreeRegisterInstruction
            val code = when {
                op == "NOP" -> ";"
                op in setOf("MOVE", "MOVE_FROM16", "MOVE_16") -> "${reg(two!!.registerA)} = ${reg(two.registerB)};"
                op in setOf("CONST_4", "CONST_16", "CONST", "CONST_HIGH16") ->
                    "${reg(one!!.registerA)} = uint32_t(${(instruction as NarrowLiteralInstruction).narrowLiteral}LL);"
                op == "RETURN" -> "return ${returnValue(method.returnType, "d2c_i32(${reg(one!!.registerA)})")};"
                op == "INT_TO_BYTE" -> "${reg(two!!.registerA)} = uint32_t(d2c_byte(${reg(two.registerB)}));"
                op == "INT_TO_SHORT" -> "${reg(two!!.registerA)} = uint32_t(d2c_short(${reg(two.registerB)}));"
                op == "INT_TO_CHAR" -> "${reg(two!!.registerA)} = ${reg(two.registerB)} & 65535u;"
                op == "NEG_INT" -> "${reg(two!!.registerA)} = 0u - ${reg(two.registerB)};"
                op == "NOT_INT" -> "${reg(two!!.registerA)} = ~${reg(two.registerB)};"
                op.startsWith("GOTO") -> "goto ${target(pos, instruction)};"
                op.startsWith("IF_") -> {
                    val cmp = when (op.substringAfter("IF_").removeSuffix("Z")) {
                        "EQ" -> "=="; "NE" -> "!="; "LT" -> "<"; "GE" -> ">="; "GT" -> ">"; "LE" -> "<="
                        else -> unsupported("opcode $op")
                    }
                    val b = if (op.endsWith("Z")) "0" else "d2c_i32(${reg(two!!.registerB)})"
                    "if (d2c_i32(${reg(one!!.registerA)}) $cmp $b) goto ${target(pos, instruction)};"
                }
                op.matches(Regex("(ADD|SUB|MUL|DIV|REM|AND|OR|XOR|SHL|SHR|USHR|RSUB)_INT(_2ADDR|_LIT8|_LIT16)?")) || op == "RSUB_INT" -> {
                    val kind = op.substringBefore("_INT")
                    val a = reg(one!!.registerA)
                    val b: String
                    val c: String
                    when {
                        op.endsWith("_2ADDR") -> { b = a; c = reg(two!!.registerB) }
                        instruction is NarrowLiteralInstruction -> {
                            b = reg(two!!.registerB); c = "uint32_t(${instruction.narrowLiteral}LL)"
                        }
                        else -> { b = reg(three!!.registerB); c = reg(three.registerC) }
                    }
                    when (kind) {
                        "DIV", "REM" -> "if ($c == 0u) return d2c_zero(env);\n$a = uint32_t(int64_t(d2c_i32($b)) ${if (kind == "DIV") "/" else "%"} int64_t(d2c_i32($c)));"
                        "RSUB" -> "$a = $c - $b;"
                        else -> "$a = ${arithmetic(kind, b, c)};"
                    }
                }
                else -> unsupported("opcode $op is not supported")
            }
            "L$pos: $code"
        }
        // Every reachable exit must be an explicit return. ART verified the original control flow;
        // the final guard is retained for malformed/unverified input, never a silent result.
        val symbol = "Java_${jniName(method.definingClass.removePrefix("L").removeSuffix(";"))}_${jniName(method.name)}__${jniName(method.parameterTypes.joinToString(""))}"
        val args = method.parameters.mapIndexed { i, p -> ", ${jniType(p.type)} p$i" }.joinToString("")
        val init = method.parameters.indices.joinToString("\n") { "r[${impl.registerCount - method.parameters.size + it}] = uint32_t(p$it);" }
        return """
            extern "C" JNIEXPORT ${jniType(method.returnType)} JNICALL $symbol(JNIEnv *env, jclass$args) {
                uint32_t r[${impl.registerCount}] = {};
                $init
                $body
                env->ThrowNew(env->FindClass("java/lang/VerifyError"), "D2C control flow fell through");
                return 0;
            }
        """.trimIndent()
    }

    private val PREAMBLE = """
        // Generated D2C: int register arithmetic uses unsigned operations for Java overflow semantics.
        #include <jni.h>
        #include <cstdint>
        #include <cstring>
        static jint d2c_i32(uint32_t value) { jint result; std::memcpy(&result, &value, 4); return result; }
        static jint d2c_byte(uint32_t value) { value &= 255u; return (value & 128u) ? jint(value) - 256 : jint(value); }
        static jint d2c_short(uint32_t value) { value &= 65535u; return (value & 32768u) ? jint(value) - 65536 : jint(value); }
        static uint32_t d2c_sar(uint32_t a, uint32_t b) {
            b &= 31u;
            if (!b) return a;
            return (a >> b) | ((a & 0x80000000u) ? (~uint32_t(0) << (32u - b)) : 0u);
        }
        static jint d2c_zero(JNIEnv *env) {
            jclass error = env->FindClass("java/lang/ArithmeticException");
            if (error) env->ThrowNew(error, "/ by zero");
            return 0;
        }

    """.trimIndent() + "\n"
}
