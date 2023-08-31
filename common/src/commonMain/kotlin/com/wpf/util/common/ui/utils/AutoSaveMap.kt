package com.wpf.util.common.ui.utils

import com.google.gson.reflect.TypeToken
import kotlin.reflect.KProperty

open class AutoSaveMap<K, V>(
    private val key: String, val value: MutableMap<K, V>
) {

    val entries: MutableSet<MutableMap.MutableEntry<K, V>> = this.value.entries
    val keys: MutableSet<K> = this.value.keys
    val size: Int = this.value.size
    val values: MutableCollection<V> = this.value.values
    fun clear() {
        this.value.clear()
        saveData()
    }

    fun isEmpty(): Boolean {
        return this.value.isEmpty()
    }

    fun putAll(from: Map<out K, V>) {
        this.value.putAll(from)
        saveData()
    }

    fun put(key: K, value: V): V? {
        val v = this.value.put(key, value)
        saveData()
        return v ?: value
    }

    fun get(key: K): V? {
        return this.value[key]
    }

    operator fun set(key: K, value: V): V? {
        return put(key, value)
    }

    fun remove(key: K): V? {
        value.remove(key)
        saveData()
        return value[key]
    }

    fun containsValue(value: V): Boolean {
        return this.value.containsValue(value)
    }

    fun containsKey(key: K): Boolean {
        return this.value.containsKey(key)
    }

    fun saveData() {
        settings.putString(key, mapGson.toJson(this.value))
    }

    inline fun <reified K, reified V> initData(key: String) {
        val oldData = gson.fromJson<MutableMap<K, V>>(
            settings.getString(key, "{}"), object : TypeToken<MutableMap<K, V>>() {}.type
        )
        oldData?.let {
            this.value.clear()
            (this.value as MutableMap<K, V>).putAll(oldData)
        }
    }
}

inline fun <reified K, reified V> autoSaveMap(
    key: String, crossinline calculation: () -> MutableMap<K, V>
) = AutoSaveMap(key, calculation()).apply {
    initData<K, V>(key)
}

internal operator fun <K, V> AutoSaveMap<K, V>.get(key: K): V? {
    return get(key)
}

internal operator fun <W, P, F> AutoSaveMap<W, MutableMap<P, F>>.get(key: W): MutableMap<P, F> {
    return get(key) ?: return put(key, mutableMapOf())!!
}

internal inline operator fun <reified K, reified V> AutoSaveMap<K, V>.getValue(
    thisObj: Any?, property: KProperty<*>
) = this

internal inline operator fun <reified K, reified V> AutoSaveMap<K, V>.setValue(
    thisObj: Any?, property: KProperty<*>, key: K, value: V
) {
    this[key] = value
    saveData()
}