package com.wpf.util.common.ui.base

import androidx.compose.runtime.MutableState

interface SelectInterface {

    val isSelectState: MutableState<Boolean>

    fun click() {

    }

    var isSelect: Boolean
    fun changeSelect(isSelect: Boolean) {
        this.isSelect = isSelect
        isSelectState.value = isSelect
    }
}