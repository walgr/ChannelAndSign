package com.wpf.util.common.ui.base

data class Menu(
    val menuName: String,
    override var isSelect: Boolean = false
): SelectItem()