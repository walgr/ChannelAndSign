package com.wpf.utils.jiagu.d2c

import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.*
import java.security.MessageDigest

/** Small integer VM. Called only after D2cCompiler validates the complete instruction stream. */
internal object VmpCompiler {
    private val arithmetic = listOf("ADD", "SUB", "MUL", "DIV", "REM", "AND", "OR", "XOR", "SHL", "SHR", "USHR", "RSUB")
    private val comparisons = listOf("EQ", "NE", "LT", "GE", "GT", "LE")

    fun translate(method: Method): String {
        val impl = requireNotNull(method.implementation)
        val instructions = impl.instructions.toList()
        val indexes = mutableMapOf<Int, Int>()
        var address = 0
        instructions.forEachIndexed { index, instruction -> indexes[address] = index; address += instruction.codeUnits }
        address = 0
        val words = instructions.flatMap { instruction ->
            val pos = address
            address += instruction.codeUnits
            val op = instruction.opcode.toString()
            val one = instruction as? OneRegisterInstruction
            val two = instruction as? TwoRegisterInstruction
            val three = instruction as? ThreeRegisterInstruction
            val row = IntArray(6)
            row[1] = one?.registerA ?: 0
            fun target() { row[5] = indexes.getValue(pos + (instruction as OffsetInstruction).codeOffset) }
            when {
                op == "NOP" -> row[0] = 0
                op in setOf("MOVE", "MOVE_FROM16", "MOVE_16") -> { row[0] = 1; row[2] = two!!.registerB }
                op in setOf("CONST_4", "CONST_16", "CONST", "CONST_HIGH16") -> {
                    row[0] = 2; row[4] = (instruction as NarrowLiteralInstruction).narrowLiteral
                }
                op == "RETURN" -> row[0] = 3
                op == "NEG_INT" || op == "NOT_INT" -> { row[0] = if (op == "NEG_INT") 4 else 5; row[2] = two!!.registerB }
                op.startsWith("GOTO") -> { row[0] = 6; target() }
                op.startsWith("IF_") -> {
                    row[0] = 7 + comparisons.indexOf(op.substringAfter("IF_").removeSuffix("Z"))
                    if (op.endsWith("Z")) row[0] = row[0] or 256 else row[3] = two!!.registerB
                    target()
                }
                else -> {
                    val kind = op.substringBefore("_INT")
                    require(kind in arithmetic) { "Unsupported VMP opcode $op" }
                    row[0] = 13 + arithmetic.indexOf(kind)
                    when {
                        op.endsWith("_2ADDR") -> { row[2] = row[1]; row[3] = two!!.registerB }
                        instruction is NarrowLiteralInstruction -> {
                            row[2] = two!!.registerB; row[4] = instruction.narrowLiteral; row[0] = row[0] or 256
                        }
                        else -> { row[2] = three!!.registerB; row[3] = three.registerC }
                    }
                }
            }
            row.toList()
        }
        // Encoding hides plain VM records in the SO; this is obfuscation, not a secret-key boundary.
        val digest = MessageDigest.getInstance("SHA-256").digest(
            (D2cCompiler.methodId(method) + words.joinToString(",")).toByteArray(Charsets.UTF_8))
        val key = (0..3).fold(0) { value, i -> value or ((digest[i].toInt() and 255) shl (8 * i)) }
        fun hex(value: Int) = "0x%08xu".format(value)
        val encoded = words.mapIndexed { i, word -> hex(word xor (key + i * 0x9e3779b9.toInt())) }
            .chunked(6).joinToString(",\n") { it.joinToString(", ") }
        val symbol = "Java_${D2cCompiler.jniName(method.definingClass.removePrefix("L").removeSuffix(";"))}_${D2cCompiler.jniName(method.name)}__${D2cCompiler.jniName(method.parameterTypes.joinToString(""))}"
        val args = method.parameters.indices.joinToString("") { ", jint p$it" }
        val init = method.parameters.indices.joinToString("\n") { "r[${impl.registerCount - method.parameters.size + it}] = uint32_t(p$it);" }
        return """
            extern "C" JNIEXPORT jint JNICALL $symbol(JNIEnv *env, jclass$args) {
                static const uint32_t program[] = {
                    $encoded
                };
                uint32_t r[${impl.registerCount}] = {};
                $init
                return vmp_run(env, r, ${impl.registerCount}u, program, ${instructions.size}u, ${hex(key)});
            }
        """.trimIndent()
    }

    val runtime = """
        // Each invocation owns its register frame and PC; recursion/concurrent calls share only const code.
        #if defined(__clang__) || defined(__GNUC__)
        __attribute__((noinline))
        #endif
        static jint vmp_run(JNIEnv *env, uint32_t *r, uint32_t registers,
                            const uint32_t *program, uint32_t count, uint32_t key) {
            uint32_t pc = 0;
            while (pc < count) {
                uint32_t w[6];
                for (uint32_t j = 0; j < 6; ++j) {
                    const uint32_t index = pc * 6u + j;
                    w[j] = program[index] ^ (key + index * 0x9e3779b9u);
                }
                ++pc;
                const uint32_t op = w[0] & 255u, a = w[1], b = w[2], c = w[3];
                if (a >= registers || b >= registers || c >= registers || (w[0] & ~511u)) break;
                const uint32_t right = (w[0] & 256u) ? w[4] : r[c];
                switch (op) {
                    case 0: break;
                    case 1: r[a] = r[b]; break;
                    case 2: r[a] = w[4]; break;
                    case 3: return d2c_i32(r[a]);
                    case 4: r[a] = 0u - r[b]; break;
                    case 5: r[a] = ~r[b]; break;
                    case 6: if (w[5] >= count) goto invalid; pc = w[5]; break;
                    case 7: case 8: case 9: case 10: case 11: case 12: {
                        const jint left = d2c_i32(r[a]), other = d2c_i32(right);
                        const bool take = op == 7 ? left == other : op == 8 ? left != other :
                            op == 9 ? left < other : op == 10 ? left >= other : op == 11 ? left > other : left <= other;
                        if (take) { if (w[5] >= count) goto invalid; pc = w[5]; }
                        break;
                    }
                    case 13: r[a] = r[b] + right; break;
                    case 14: r[a] = r[b] - right; break;
                    case 15: r[a] = r[b] * right; break;
                    case 16: case 17:
                        if (!right) return d2c_zero(env);
                        r[a] = uint32_t(op == 16 ? int64_t(d2c_i32(r[b])) / int64_t(d2c_i32(right)) :
                                                  int64_t(d2c_i32(r[b])) % int64_t(d2c_i32(right)));
                        break;
                    case 18: r[a] = r[b] & right; break;
                    case 19: r[a] = r[b] | right; break;
                    case 20: r[a] = r[b] ^ right; break;
                    case 21: r[a] = r[b] << (right & 31u); break;
                    case 22: r[a] = d2c_sar(r[b], right); break;
                    case 23: r[a] = r[b] >> (right & 31u); break;
                    case 24: r[a] = right - r[b]; break;
                    default: goto invalid;
                }
            }
        invalid:
            jclass error = env->FindClass("java/lang/VerifyError");
            if (error) env->ThrowNew(error, "Invalid virtualized method program");
            return 0;
        }

    """.trimIndent() + "\n"
}
