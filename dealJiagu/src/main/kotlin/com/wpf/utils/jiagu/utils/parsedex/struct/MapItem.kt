package com.wpf.utils.jiagu.utils.parsedex.struct

class MapItem {
    /**
     * struct map_item
     * {
     * ushort type;
     * ushort unuse;
     * uint size;
     * uint offset;
     * }
     */
    var type: Short = 0
    var unuse: Short = 0
    var size: Int = 0
    var offset: Int = 0

    override fun toString(): String {
        return "type:$type,unuse:$unuse,size:$size,offset:$offset"
    }

    companion object {
        fun getSize(): Int {
            return 2 + 2 + 4 + 4
        }
    }
}
