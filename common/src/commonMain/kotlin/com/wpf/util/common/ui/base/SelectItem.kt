package com.wpf.util.common.ui.base

import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class SelectItem(
    @Transient open var isSelect: Boolean = false
) {
    @Transient
    var isSelectState = mutableStateOf(isSelect)
        get() {
            field.value = isSelect
            return field
        }
}