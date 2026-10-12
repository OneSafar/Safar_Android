package com.safarparmar.app.data.repository

import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.util.Resource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Short-lived, in-memory snapshots for explicitly selected, bounded summary reads only. */
@Singleton
class ReadSnapshotStore @Inject constructor(private val dataStore: SafarDataStore) {
    private data class Entry(val value: Any, val createdNanos: Long)
    private val lock = Any()
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private val requestLocks = mutableMapOf<String, Mutex>()
    private var owner: String? = null
    private var generation = 0L

    fun clear() = synchronized(lock) {
        entries.clear()
        owner = null
        generation++
        Unit
    }

    suspend fun <T : Any> read(key: String, request: suspend () -> Resource<T>): Resource<T> {
        val mutex = synchronized(lock) { requestLocks.getOrPut(key) { Mutex() } }
        return mutex.withLock {
            val token = dataStore.authToken.first()
            if (token.isNullOrBlank()) return@withLock request()
            val epoch = synchronized(lock) {
                if (owner != token) { entries.clear(); owner = token; generation++ }
                val entry = entries[key]
                if (entry != null && System.nanoTime() - entry.createdNanos < 60_000_000_000L) {
                    // Each private call site owns a fixed key and result type.
                    @Suppress("UNCHECKED_CAST")
                    return@withLock Resource.Success(entry.value as T)
                }
                generation
            }
            val result = request()
            if (result is Resource.Success) synchronized(lock) {
                // A write or account change must not repopulate the cache with an older read.
                if (owner == token && generation == epoch) {
                    entries[key] = Entry(result.data, System.nanoTime())
                    while (entries.size > 16) entries.remove(entries.keys.first())
                }
            }
            result
        }
    }
}
