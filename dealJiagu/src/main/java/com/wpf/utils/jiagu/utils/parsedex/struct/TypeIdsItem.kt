package com.wpf.utils.jiagu.utils.parsedex.struct

import com.wpf.utils.jiagu.utils.parsedex.Utils

class TypeIdsItem {
    /**
     * struct type_ids_item
     * {
     * uint descriptor_idx;
     * }
     */
    var descriptor_idx: Int = 0

    override fun toString(): String {
        return Utils.bytesToHexString(Utils.int2Byte(descriptor_idx)) ?: ""
    }

    companion object {
        fun getSize() = 4
    }
}
