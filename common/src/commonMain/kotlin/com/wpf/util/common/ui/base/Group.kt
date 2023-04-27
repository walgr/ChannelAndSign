package com.wpf.util.common.ui.base

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class Group(
    @Transient open var name: String = "",
    @Transient override var isSelect: Boolean = false,
): SelectItem(isSelect) {

    @Transient
    open var nameState: MutableState<String> = mutableStateOf(name)
        get() {
            field.value = name
            return field
        }
}