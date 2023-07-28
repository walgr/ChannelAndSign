package com.wpf.util.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.reflect.KProperty

class AutoSave<T : Any, H : MutableState<T>>(
    private val key: String, var data: H
) {

    var value: T
        get() {
            return data.value
        }
        set(value) {
            data.value = value
            settings.putString(key, gson.toJson(value))
        }

    init {
        val json = settings.getString(key, "")
        val t = gson.fromJson(json, data.value.javaClass)
        t?.let {
            data.value = t
        }
    }
}

internal inline operator fun <reified T : Any, H : MutableState<T>> AutoSave<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = data

//internal operator fun <T : Any, H : MutableState<T>> AutoSave<T, H>.setValue(
//    thisObj: Any?,
//    property: KProperty<*>,
//    value: T
//) {
//    this.value = value
//}

inline fun <reified T : Any, H : MutableState<T>> autoSave(
    key: String, crossinline calculation: () -> H
) = AutoSave(key, calculation())

@Composable
inline fun <reified T : Any, H : MutableState<T>> autoSaveComposable(
    key: String, crossinline calculation: @Composable () -> H
) = AutoSave(key, calculation())

class AutoSaveList<T : Any, H : SnapshotStateList<T>>(
    private val key: String, var data: H
) {

    val size: Int = data.size

    fun remove(t: T) {
        data.remove(t)
        saveData()
    }

    fun add(t: T) {
        data.add(t)
        saveData()
    }

    private fun saveData() {
        settings.putString(key, gson.toJson(data))
    }

    init {
        val json = settings.getString(key, "")
        val t = gson.fromJson(json, data.javaClass)
        t?.let {
            data = t
        }
    }
}

internal inline operator fun <reified T : Any, H : SnapshotStateList<T>> AutoSaveList<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = data

internal inline operator fun <reified T : Any, H : SnapshotStateList<T>> AutoSaveList<T, H>.setValue(
    thisObj: Any?, property: KProperty<*>, value: H
) {
    this.data = value
}

inline fun <reified T : Any, H : SnapshotStateList<T>> autoSaveList(
    key: String, crossinline calculation: () -> H
) = AutoSaveList(key, calculation())

@Composable
inline fun <reified T : Any, H : SnapshotStateList<T>> autoSaveListComposable(
    key: String, crossinline calculation: @Composable () -> H
) = AutoSaveList(key, calculation())
