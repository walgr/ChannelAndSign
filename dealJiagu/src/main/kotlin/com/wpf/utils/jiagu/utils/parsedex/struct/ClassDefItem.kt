package com.wpf.utils.jiagu.utils.parsedex.struct

class ClassDefItem {
    /**
     * struct class_def_item
     * {
     * uint class_idx;
     * uint access_flags;
     * uint superclass_idx;
     * uint interfaces_off;
     * uint source_file_idx;
     * uint annotations_off;
     * uint class_data_off;
     * uint static_value_off;
     * }
     */
    var class_idx: Int = 0
    var access_flags: Int = 0
    var superclass_idx: Int = 0
    var iterfaces_off: Int = 0
    var source_file_idx: Int = 0
    var annotations_off: Int = 0
    var class_data_off: Int = 0
    var static_value_off: Int = 0

    override fun toString(): String {
        return ("class_idx:" + class_idx + ",access_flags:" + access_flags + ",superclass_idx:" + superclass_idx + ",iterfaces_off:" + iterfaces_off
                + ",source_file_idx:" + source_file_idx + ",annotations_off:" + annotations_off + ",class_data_off:" + class_data_off
                + ",static_value_off:" + static_value_off)
    }

    companion object {
        const val ACC_PUBLIC: Int = 0x00000001 // class, field, method, ic
        const val ACC_PRIVATE: Int = 0x00000002 // field, method, ic
        const val ACC_PROTECTED: Int = 0x00000004 // field, method, ic
        const val ACC_STATIC: Int = 0x00000008 // field, method, ic
        const val ACC_FINAL: Int = 0x00000010 // class, field, method, ic
        const val ACC_SYNCHRONIZED: Int = 0x00000020 // method (only allowed on natives)
        const val ACC_SUPER: Int = 0x00000020 // class (not used in Dalvik)
        const val ACC_VOLATILE: Int = 0x00000040 // field
        const val ACC_BRIDGE: Int = 0x00000040 // method (1.5)
        const val ACC_TRANSIENT: Int = 0x00000080 // field
        const val ACC_VARARGS: Int = 0x00000080 // method (1.5)
        const val ACC_NATIVE: Int = 0x00000100 // method
        const val ACC_INTERFACE: Int = 0x00000200 // class, ic
        const val ACC_ABSTRACT: Int = 0x00000400 // class, method, ic
        const val ACC_STRICT: Int = 0x00000800 // method
        const val ACC_SYNTHETIC: Int = 0x00001000 // field, method, ic
        const val ACC_ANNOTATION: Int = 0x00002000 // class, ic (1.5)
        const val ACC_ENUM: Int = 0x00004000 // class, field, ic (1.5)
        const val ACC_CONSTRUCTOR: Int = 0x00010000 // method (Dalvik only)
        const val ACC_DECLARED_SYNCHRONIZED: Int = 0x00020000 // method (Dalvik only)
        const val ACC_CLASS_MASK: Int = (ACC_PUBLIC or ACC_FINAL or ACC_INTERFACE or ACC_ABSTRACT
                or ACC_SYNTHETIC or ACC_ANNOTATION or ACC_ENUM)
        const val ACC_INNER_CLASS_MASK: Int = (ACC_CLASS_MASK or ACC_PRIVATE or ACC_PROTECTED or ACC_STATIC)
        const val ACC_FIELD_MASK: Int = (ACC_PUBLIC or ACC_PRIVATE or ACC_PROTECTED or ACC_STATIC or ACC_FINAL
                or ACC_VOLATILE or ACC_TRANSIENT or ACC_SYNTHETIC or ACC_ENUM)
        const val ACC_METHOD_MASK: Int = (ACC_PUBLIC or ACC_PRIVATE or ACC_PROTECTED or ACC_STATIC or ACC_FINAL
                or ACC_SYNCHRONIZED or ACC_BRIDGE or ACC_VARARGS or ACC_NATIVE
                or ACC_ABSTRACT or ACC_STRICT or ACC_SYNTHETIC or ACC_CONSTRUCTOR
                or ACC_DECLARED_SYNCHRONIZED)

        fun getSize() = 4 * 8
    }
}
