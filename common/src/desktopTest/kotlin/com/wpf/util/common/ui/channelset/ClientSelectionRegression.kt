package com.wpf.util.common.ui.channelset

import androidx.compose.runtime.mutableStateListOf
import com.wpf.util.common.ui.utils.AutoSaveList
import com.wpf.util.common.ui.utils.settings
import java.util.UUID

fun main() {
    // Use the real wrapper and snapshots, with a disposable key unrelated to user settings.
    val key = "regression-client-selection-${UUID.randomUUID()}"
    val clients = AutoSaveList(key, mutableStateListOf<Client>())
    val files = mutableStateListOf("old.apk")
    val names = mutableStateListOf("old-channel")
    try {
        dealClientClick(clients, 0, files, names)
        check(files.isEmpty() && names.isEmpty())

        val first = Client("first", "First")
        val second = Client("second", "Second")
        clients.addAll(listOf(first, second))
        dealClientClick(clients, 1, files, names)
        check(second.isSelect && second.isSelectState.value)
        check(!first.isSelect && !first.isSelectState.value)

        files.add("current.apk"); names.add("current-channel")
        for (staleIndex in listOf(-1, clients.size, Int.MAX_VALUE)) {
            dealClientClick(clients, staleIndex, files, names)
            check(second.isSelect && second.isSelectState.value)
            check(files.single() == "current.apk" && names.single() == "current-channel")
        }

        clients.remove(second)
        dealClientClick(clients, 0, files, names)
        check(first.isSelect && first.isSelectState.value && files.isEmpty() && names.isEmpty())
        val restored = AutoSaveList(key, mutableStateListOf<Client>()).apply { initData<Client>(key) }
        check(restored.single().id == first.id && restored.single().isSelect)

        files.add("stale.apk"); names.add("stale-channel")
        clients.remove(first)
        dealClientClick(clients, 0, files, names)
        check(clients.isEmpty() && files.isEmpty() && names.isEmpty())
        check(AutoSaveList(key, mutableStateListOf<Client>()).apply { initData<Client>(key) }.isEmpty())
        println("CLIENT_SELECTION_PASSED: empty list, last deletion, remaining client selection, stale indexes and persisted state")
    } finally {
        settings.remove(key)
    }
}
