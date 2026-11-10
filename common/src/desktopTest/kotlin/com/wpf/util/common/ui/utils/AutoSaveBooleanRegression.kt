package com.wpf.util.common.ui.utils

import androidx.compose.runtime.mutableStateOf
import com.russhwolf.settings.Settings
import java.lang.reflect.Proxy

fun main() {
    // Keep the real persistence wrappers; replace only the storage, never touching user preferences.
    val values = mutableMapOf<String, String>()
    val storage = Proxy.newProxyInstance(Settings::class.java.classLoader, arrayOf(Settings::class.java)) { _, method, args ->
        when (method.name) {
            "getString" -> values[args!![0] as String] ?: args[1] as String
            "putString" -> { values[args!![0] as String] = args[1] as String; null }
            else -> error("Unexpected settings call: ${method.name}")
        }
    } as Settings
    val key = "jiaguResourcePaths"
    for (saved in listOf(null, "", "true", "false", "invalid")) {
        for (default in listOf(false, true)) {
            values.clear(); saved?.let { values[key] = it }
            val expected = when (saved) { "true" -> true; "false" -> false; else -> default }
            val state = AutoSaveState(key, mutableStateOf(default), storage)
            val plain = AutoSaveAny(key, default, storage)
            check(state.value == expected && plain.value == expected)
            state.value = !expected
            check(AutoSaveState(key, mutableStateOf(expected), storage).value == !expected)
            plain.value = expected
            check(AutoSaveAny(key, !expected, storage).value == expected)
        }
    }
    values[key] = "SDK path"
    check(AutoSaveState(key, mutableStateOf(""), storage).value == "SDK path")
    check(AutoSaveAny(key, "", storage).value == "SDK path")
    println("AUTOSAVE_BOOLEAN_PASSED: absent/empty/invalid defaults, true/false restore, toggle/save/reload, unchanged string settings")
}
