package com.wpf.util.common.ui.utils

interface AutoSave {

    fun keyMove(oldKey: String, newKey: String) {
        val data = settings.getString(oldKey, "")
        if (data.isNotEmpty()) {
            settings.putString(newKey, data)
            settings.remove(oldKey)
        }
    }
}