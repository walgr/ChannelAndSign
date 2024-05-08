package com.wpf.utils.jiagu.utils.parsedex.struct

import com.wpf.utils.jiagu.utils.parsedex.Utils

class EncodedMethod(val classIdx: String? = null) {
    /**
     * struct encoded_method
     * {
     * uleb128 method_idx_diff;
     * uleb128 access_flags;
     * uleb128 code_off;
     * }
     */
    var method_idx_diff: ByteArray = ByteArray(0)
    var access_flags: ByteArray = ByteArray(0)
    var code_off: ByteArray = ByteArray(0)

    override fun toString(): String {
        return ((((("method_idx_diff:" + Utils.bytesToHexString(method_idx_diff)).toString() + "," + Utils.bytesToHexString(
            Utils.int2Byte(Utils.decodeULeb128(method_idx_diff))
        )
                ).toString() + ",access_flags:" + Utils.bytesToHexString(access_flags)).toString() + "," + Utils.bytesToHexString(
            Utils.int2Byte(Utils.decodeULeb128(access_flags))
        )
                ).toString() + ",code_off:" + Utils.bytesToHexString(code_off)).toString() + "," + Utils.bytesToHexString(
            Utils.int2Byte(Utils.decodeULeb128(code_off))
        )
    }
}
