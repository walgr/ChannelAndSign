package com.wpf.utils.jiagu.utils.parsedex.struct

class MethodIdsItem {
    /**
     * struct filed_id_item
     * {
     * ushort class_idx;
     * ushort proto_idx;
     * uint name_idx;
     * }
     */
    var class_idx: Short = 0
    var proto_idx: Short = 0
    var name_idx: Int = 0

    override fun toString(): String {
        return "class_idx:$class_idx,proto_idx:$proto_idx,name_idx:$name_idx"
    }

    companion object {
        fun getSize() = 2 + 2 + 4
    }
}
