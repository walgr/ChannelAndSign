package com.wpf.utils.jiagu.utils.parsedex.struct

class TypeList {
    /**
     * struct type_list
     * {
     * uint size;
     * ushort type_idx[size];
     * }
     */
    var size: Int = 0 //�����ĸ���
    var type_idx: List<Short> = ArrayList() //����������
}
