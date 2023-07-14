package com.wpf.util.common.ui.base

import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class SelectItem(
    @Transient open var isSelect: Boolean = false
) {
    @Transient
    open var isSelectState = mutableStateOf(isSelect)
        get() {
            field.value = isSelect
            return field
        }
        set(value) {
            field.value = value.value
            isSelect = field.value
            field = value
        }

    open fun click(): Boolean {
        isSelect = !isSelect
        isSelectState.value = isSelect
        return isSelect
    }

    fun changeSelect(isSelect: Boolean) {
        this.isSelect = isSelect
        isSelectState.value = isSelect
    }
}