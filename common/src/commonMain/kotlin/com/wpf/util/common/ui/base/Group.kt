package com.wpf.util.common.ui.base

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class Group(
    @kotlin.jvm.Transient @Transient open var name: String = "",
    @kotlin.jvm.Transient @Transient override var isSelect: Boolean = false,
): SelectItem(isSelect) {

    @kotlin.jvm.Transient @Transient
    open var nameState: MutableState<String> = mutableStateOf(name)
        get() {
            if (field == null) field = mutableStateOf(name)
            field.value = name
            return field
        }
}