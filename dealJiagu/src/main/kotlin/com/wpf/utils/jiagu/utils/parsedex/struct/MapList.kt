package com.wpf.utils.jiagu.utils.parsedex.struct

class MapList {
    /**
     * struct maplist
     * {
     * uint size;
     * map_item list [size];
     * }
     */
    var size: Int = 0
    var map_item: ArrayList<MapItem> = ArrayList()
}
