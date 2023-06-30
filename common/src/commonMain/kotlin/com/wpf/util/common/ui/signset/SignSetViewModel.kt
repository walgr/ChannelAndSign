package com.wpf.util.common.ui.signset

import com.russhwolf.settings.set
import com.wpf.util.common.ui.utils.json
import com.wpf.util.common.ui.utils.settings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object SignSetViewModel {

    fun getSignList(): List<SignFile> {
        settings.getStringOrNull("signList")?.let {
            return json.decodeFromString(it)
        }
        return arrayListOf()
    }

    fun saveSignList(signList: List<SignFile>) {
        settings["signList"] = json.encodeToString(signList)
    }
}