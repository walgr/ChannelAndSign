package com.wpf.utils.jiagu.utils.parsedex.struct

class ProtoIdsItem {
    /**
     * struct proto_id_item
     * {
     * uint shorty_idx;
     * uint return_type_idx;
     * uint parameters_off;
     * }
     */
    var shorty_idx: Int = 0
    var return_type_idx: Int = 0
    var parameters_off: Int = 0

    //������ǹ����ֶΣ�����Ϊ�˴洢����ԭ���еĲ����������Ͳ�������
    var parametersList: List<String> = ArrayList()
    var parameterCount: Int = 0

    override fun toString(): String {
        return "shorty_idx:$shorty_idx,return_type_idx:$return_type_idx,parameters_off:$parameters_off"
    }

    companion object {
        fun getSize() = 4 + 4 + 4
    }
}
