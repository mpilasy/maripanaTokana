// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps the last loaded value for [ttlMs] while the key stays the same; callers at the same time share one load. */
class ExpiringValue<K, V>(
    private val ttlMs: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private class Entry<K, V>(val key: K, val loadedAt: Long, val value: V)

    private val mutex = Mutex()
    private var entry: Entry<K, V>? = null

    suspend fun get(key: K, load: suspend () -> V): V = mutex.withLock {
        entry?.takeIf { it.key == key && now() - it.loadedAt < ttlMs }?.value
            ?: load().also { entry = Entry(key, now(), it) }
    }
}
