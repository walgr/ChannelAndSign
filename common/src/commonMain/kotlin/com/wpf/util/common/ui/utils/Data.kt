package com.wpf.util.common.ui.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

val gson = Gson()
val mapGson = GsonBuilder().disableHtmlEscaping().create()
val json = Json { encodeDefaults = true }
val settings = Settings()