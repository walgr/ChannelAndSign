package com.wpf.util.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.reflect.TypeToken
import kotlin.reflect.KProperty

class AutoSaveSet<T, H : MutableSet<T>>(
    private val key: String, var value: H
) : MutableSet<T> by value {

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
        val t = gson.fromJson<Set<T>>(json, object : TypeToken<Set<T>>() {}.type)
        t?.let {
            value.clear()
            (value as MutableSet<T>).addAll(it)
        }
    }
}

internal inline operator fun <reified T, reified H : MutableSet<T>> AutoSaveSet<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = this

internal inline operator fun <reified T, reified H : MutableSet<T>> AutoSaveSet<T, H>.setValue(
    thisObj: Any?, property: KProperty<*>, value: H
) {
    this.value = value
    saveData()
}

inline fun <reified T, reified H : MutableSet<T>> autoSaveSet(
    key: String, crossinline calculation: () -> H
) = AutoSaveSet(key, calculation()).apply {
    initData<T>(key)
}

class AutoSaveList<T, H : MutableList<T>>(
    private val key: String, var value: H
) : MutableList<T> by value {

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
        if (json.isNotEmpty()) {
                val t = gson.fromJson<List<T>>(json, object : TypeToken<List<T>>() {}.type)
                t?.let {
                    value.clear()
                    (value as MutableList<T>).addAll(it)
                }
        }
    }
}

internal inline operator fun <reified T, reified H : MutableList<T>> AutoSaveList<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = this

internal inline operator fun <reified T, reified H : MutableList<T>> AutoSaveList<T, H>.setValue(
    thisObj: Any?, property: KProperty<*>, value: H
) {
    this.value = value
    saveData()
}

inline fun <reified T, reified H : SnapshotStateList<T>> autoSaveList(
    key: String, crossinline calculation: () -> H
) = AutoSaveList(key, calculation()).apply {
    initData<T>(key)
}

@Composable
inline fun <reified T, reified H : SnapshotStateList<T>> autoSaveListComposable(
    key: String, crossinline calculation: @Composable () -> H
) = AutoSaveList(key, calculation()).apply {
    initData<T>(key)
}


