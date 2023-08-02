package com.wpf.util.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.reflect.TypeToken
import kotlin.reflect.KProperty

class AutoSaveListState<T, H : SnapshotStateList<T>>(
    private val key: String, var value: H
): MutableList<T> by value {

    override fun remove(element: T): Boolean {
        val b = value.remove(element)
        saveData()
        return b
    }

    override fun add(element: T): Boolean {
        val b = value.add(element)
        saveData()
        return b
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val b = value.addAll(elements)
        saveData()
        return b
    }

    fun saveData() {
        settings.putString(key, gson.toJson(value))
    }

    inline fun <reified T> initData(key: String) {
        val json = settings.getString(key, "[]")
        val t = gson.fromJson<List<T>>(json, object : TypeToken<List<T>>() {}.type)
        t?.let {
            value.clear()
            (value as SnapshotStateList<T>).addAll(it)
        }
    }
}

internal inline operator fun <reified T, H : SnapshotStateList<T>> AutoSaveListState<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = this

internal inline operator fun <reified T, H : SnapshotStateList<T>> AutoSaveListState<T, H>.setValue(
    thisObj: Any?, property: KProperty<*>, value: H
) {
    this.value = value
}

inline fun <reified T, H : SnapshotStateList<T>> autoSaveList(
    key: String, crossinline calculation: () -> H
) = AutoSaveListState(key, calculation()).apply {
    initData<T>(key)
}

@Composable
inline fun <reified T> autoSaveListComposable(
    key: String, crossinline calculation: @Composable () -> SnapshotStateList<T>
) = AutoSaveListState(key, calculation()).apply {
    initData<T>(key)
}


