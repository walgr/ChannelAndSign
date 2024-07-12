package com.wpf.utils.jiagu.utils.parsedex.struct

import com.wpf.utils.jiagu.utils.parsedex.Utils

class EncodedField {
    /**
     * struct encoded_field
     * {
     * uleb128 filed_idx_diff; // index into filed_ids for ID of this filed
     * uleb128 access_flags; // access flags like public, static etc.
     * }
     */
    var filed_idx_diff: ByteArray = ByteArray(0)
    var access_flags: ByteArray = ByteArray(0)

    override fun toString(): String {
        return ("field_idx_diff:" + Utils.bytesToHexString(filed_idx_diff)).toString() + ",access_flags:" + Utils.bytesToHexString(
            filed_idx_diff
        )
    }
}
