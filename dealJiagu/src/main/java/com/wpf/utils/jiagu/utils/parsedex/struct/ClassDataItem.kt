package com.wpf.utils.jiagu.utils.parsedex.struct

class ClassDataItem(val classIdx: String? = null) {
    /**
     * uleb128 unsigned little-endian base 128
     * struct class_data_item
     * {
     * uleb128 static_fields_size;
     * uleb128 instance_fields_size;
     * uleb128 direct_methods_size;
     * uleb128 virtual_methods_size;
     * encoded_field static_fields [ static_fields_size ];
     * encoded_field instance_fields [ instance_fields_size ];
     * encoded_method direct_methods [ direct_method_size ];
     * encoded_method virtual_methods [ virtual_methods_size ];
     * }
     */
    //uleb128ֻ��������32λ��������
    var static_fields_size: Int = 0
    var instance_fields_size: Int = 0
    var direct_methods_size: Int = 0
    var virtual_methods_size: Int = 0

    var static_fields: Array<EncodedField> = arrayOf()
    var instance_fields: Array<EncodedField> = arrayOf()
    var direct_methods: Array<EncodedMethod> = arrayOf()
    var virtual_methods: Array<EncodedMethod> = arrayOf()

    override fun toString(): String {
        return """
            static_fields_size:$static_fields_size,instance_fields_size:$instance_fields_size,direct_methods_size:$direct_methods_size,virtual_methods_size:$virtual_methods_size
            ${fieldsAndMethods}
            """.trimIndent()
    }

    private val fieldsAndMethods: String
        get() {
            val sb = StringBuilder()
            sb.append("static_fields:\n")
            for (i in static_fields.indices) {
                sb.append(static_fields[i].toString() + "\n")
            }
            sb.append("instance_fields:\n")
            for (i in instance_fields.indices) {
                sb.append(instance_fields[i].toString() + "\n")
            }
            sb.append("direct_methods:\n")
            for (i in direct_methods.indices) {
                sb.append(direct_methods[i].toString() + "\n")
            }
            sb.append("virtual_methods:\n")
            for (i in virtual_methods.indices) {
                sb.append(virtual_methods[i].toString() + "\n")
            }
            return sb.toString()
        }
}
