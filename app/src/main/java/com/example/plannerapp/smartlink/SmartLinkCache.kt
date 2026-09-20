package com.example.plannerapp.smartlink

import android.content.Context
import android.util.LruCache
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dual-level cache (In-memory LRU + Disk JSON) for instantaneous, offline-ready link previews.
 */
object SmartLinkCache {

    private const val MEMORY_CACHE_SIZE = 200
    private const val DISK_CACHE_DIR = "smart_links"
    private const val DISK_CACHE_FILE = "metadata_cache.json"

    private val memoryCache = LruCache<String, SmartLinkMetadata>(MEMORY_CACHE_SIZE)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isDiskLoaded = false
    private var cacheDir: File? = null

    /**
     * Initializes disk caching directory and eagerly warms memory cache from disk.
     */
    fun init(context: Context) {
        if (isDiskLoaded) return
        val dir = File(context.cacheDir, DISK_CACHE_DIR).apply { mkdirs() }
        cacheDir = dir
        scope.launch {
            try {
                val file = File(dir, DISK_CACHE_FILE)
                if (file.exists()) {
                    val json = file.readText()
                    val type = object : TypeToken<Map<String, SmartLinkMetadata>>() {}.type
                    val diskMap: Map<String, SmartLinkMetadata>? = gson.fromJson(json, type)
                    diskMap?.forEach { (url, metadata) ->
                        memoryCache.put(url, metadata)
                    }
                }
                isDiskLoaded = true
            } catch (e: Exception) {
                // Ignore corrupt cache file
            }
        }
    }

    /**
     * Retrieves cached metadata for a URL, if available.
     */
    fun get(url: String): SmartLinkMetadata? {
        return memoryCache.get(url)
    }

    /**
     * Stores metadata in memory cache and flushes to disk asynchronously.
     */
    fun put(metadata: SmartLinkMetadata) {
        memoryCache.put(metadata.url, metadata)
        persistToDisk()
    }

    private fun persistToDisk() {
        val dir = cacheDir ?: return
        scope.launch {
            try {
                val snapshot = memoryCache.snapshot()
                val json = gson.toJson(snapshot)
                val file = File(dir, DISK_CACHE_FILE)
                file.writeText(json)
            } catch (e: Exception) {
                // Disk write best-effort
            }
        }
    }
}
