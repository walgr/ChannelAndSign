package com.wpf.utils.jiagu.utils.parsedex.struct

import com.wpf.utils.jiagu.utils.parsedex.Utils

class CodeItem(classIdx: String? = null) {
    /**
     * struct code_item
     * {
     * ushort registers_size;
     * ushort ins_size;
     * ushort outs_size;
     * ushort tries_size;
     * uint debug_info_off;
     * uint insns_size;
     * ushort insns [ insns_size ];
     * ushort paddding; // optional
     * try_item tries [ tyies_size ]; // optional
     * encoded_catch_handler_list handlers; // optional
     * }
     */
    var registers_size: Short = 0
    var ins_size: Short = 0
    var outs_size: Short = 0
    var tries_size: Short = 0
    var debug_info_off: Int = 0
    var insns_size: Int = 0
    var insns: ShortArray = shortArrayOf()

    //指令偏移
    var insnsOffset: Int = 0

    override fun toString(): String {
        return """
            regsize:$registers_size,ins_size:$ins_size,outs_size:$outs_size,tries_size:$tries_size,debug_info_off:$debug_info_off,insns_size:$insns_size
            insns:${insnsStr}
            """.trimIndent()
    }

    private val insnsStr: String
        get() {
            val sb = StringBuilder()
            for (i in insns.indices) {
                sb.append(Utils.bytesToHexString(Utils.short2Byte(insns[i])) + ",")
            }
            return sb.toString()
        }
}
