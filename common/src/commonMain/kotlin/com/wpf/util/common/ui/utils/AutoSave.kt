package com.wpf.util.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty

class AutoSave<T>(
    private val key: String, private var data: T
) {

    var value: T = data
        get() = data
        set(value) {
            field = value
            data = value
            settings.putString(key, if (value is Serializable) gson.toJson(value) else value.toString())
        }

    init {
        val json = settings.getString(key, "")
        if (data is Serializable) {
            val t = gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
            t?.let {
                data = it
            }
        } else {
            this.data = json as T
        }
    }
}

inline fun <reified T> autoSave(
    key: String, crossinline calculation: () -> T
) = AutoSave(key, calculation())

internal inline operator fun <reified T> AutoSave<T>.getValue(
    thisObj: Any?, property: KProperty<*>
) = value

internal inline operator fun <reified T> AutoSave<T>.setValue(
    thisObj: Any?, property: KProperty<*>, value: T
) {
    this.value = value
}

class AutoSaveState<T, H : MutableState<T>>(
    private var key: String, value: H
): MutableState<T> by value {
    private var data: H = value

    override var value: T
        get() = data.value
        set(value) {
            this.data.value = value
            settings.putString(key, if (value is Serializable) gson.toJson(value) else value.toString())
        }

    init {
        val json = settings.getString(key, "")
        if (data.value is Serializable) {
            val t = gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
            t?.let {
                this.data.value = it
            }
        } else {
            this.data.value = json as T
        }
    }
}

@Composable
inline fun <reified T, H : MutableState<T>> autoSaveComposable(
    key: String, crossinline calculation: @Composable () -> H
) = AutoSaveState(key, calculation())

internal inline operator fun <reified T, H : MutableState<T>> AutoSaveState<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = this

internal inline operator fun <reified T, H : MutableState<T>> AutoSaveState<T, H>.setValue(
    thisObj: Any?, property: KProperty<*>, value: T
) {
    this.value = value
}