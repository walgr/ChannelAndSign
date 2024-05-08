package com.wpf.utils.jiagu.utils.parsedex.struct

import com.wpf.utils.jiagu.utils.parsedex.Utils

class StringIdsItem {
    /**
     * struct string_ids_item
     * {
     * uint string_data_off;
     * }
     */
    var string_data_off: Int = 0

    override fun toString(): String {
        return Utils.bytesToHexString(Utils.int2Byte(string_data_off)) ?: ""
    }

    companion object {
        fun getSize() = 4
    }
}
