package com.wpf.utils.jiagu.utils.parsedex.struct

import com.wpf.utils.jiagu.utils.parsedex.Utils

class HeaderType {
    /**
     * struct header_item
     * {
     * ubyte[8] magic;
     * unit checksum;
     * ubyte[20] siganature;
     * uint file_size;
     * uint header_size;
     * unit endian_tag;
     * uint link_size;
     * uint link_off;
     * uint map_off;
     * uint string_ids_size;
     * uint string_ids_off;
     * uint type_ids_size;
     * uint type_ids_off;
     * uint proto_ids_size;
     * uint proto_ids_off;
     * uint method_ids_size;
     * uint method_ids_off;
     * uint class_defs_size;
     * uint class_defs_off;
     * uint data_size;
     * uint data_off;
     * }
     */
    var magic: ByteArray? = ByteArray(8)
    var checksum: Int = 0
    var siganature: ByteArray? = ByteArray(20)
    var file_size: Int = 0
    var header_size: Int = 0
    var endian_tag: Int = 0
    var link_size: Int = 0
    var link_off: Int = 0
    var map_off: Int = 0
    var string_ids_size: Int = 0
    var string_ids_off: Int = 0
    var type_ids_size: Int = 0
    var type_ids_off: Int = 0
    var proto_ids_size: Int = 0
    var proto_ids_off: Int = 0
    var field_ids_size: Int = 0
    var field_ids_off: Int = 0
    var method_ids_size: Int = 0
    var method_ids_off: Int = 0
    var class_defs_size: Int = 0
    var class_defs_off: Int = 0
    var data_size: Int = 0
    var data_off: Int = 0

    override fun toString(): String {
        return ((((((((("""${"magic:" + Utils.bytesToHexString(magic)}
checksum:$checksum
siganature:${Utils.bytesToHexString(siganature)}""".toString() + "\n"
                + "file_size:" + file_size + "\n"
                + "header_size:" + header_size + "\n"
                + "endian_tag:" + endian_tag + "\n"
                + "link_size:" + link_size + "\n"
                + "link_off:" + Utils.bytesToHexString(Utils.int2Byte(link_off))).toString() + "\n"
                + "map_off:" + Utils.bytesToHexString(Utils.int2Byte(map_off))).toString() + "\n"
                + "string_ids_size:" + string_ids_size + "\n"
                + "string_ids_off:" + Utils.bytesToHexString(Utils.int2Byte(string_ids_off))).toString() + "\n"
                + "type_ids_size:" + type_ids_size + "\n"
                + "type_ids_off:" + Utils.bytesToHexString(Utils.int2Byte(type_ids_off))).toString() + "\n"
                + "proto_ids_size:" + proto_ids_size + "\n"
                + "proto_ids_off:" + Utils.bytesToHexString(Utils.int2Byte(proto_ids_off))).toString() + "\n"
                + "field_ids_size:" + field_ids_size + "\n"
                + "field_ids_off:" + Utils.bytesToHexString(Utils.int2Byte(field_ids_off))).toString() + "\n"
                + "method_ids_size:" + method_ids_size + "\n"
                + "method_ids_off:" + Utils.bytesToHexString(Utils.int2Byte(method_ids_off))).toString() + "\n"
                + "class_defs_size:" + class_defs_size + "\n"
                + "class_defs_off:" + Utils.bytesToHexString(Utils.int2Byte(class_defs_off))).toString() + "\n"
                + "data_size:" + data_size + "\n"
                + "data_off:" + Utils.bytesToHexString(Utils.int2Byte(data_off)))
    }
}
