package com.pandulapeter.campfire.data.repository

import com.pandulapeter.campfire.data.model.SongInfo
import com.pandulapeter.campfire.data.network.NetworkManager
import com.pandulapeter.campfire.data.storage.StorageManager
import com.pandulapeter.campfire.util.enqueueCall
import java.util.Collections

/**
 * Wraps caching and updating of [SongInfo] objects.
 */
class SongInfoRepository(private val storageManager: StorageManager, private val networkManager: NetworkManager) {
    private var subscribers = mutableSetOf<Subscriber>()
    private var dataSet = storageManager.cloudCache
        get() {
            if (System.currentTimeMillis() - storageManager.lastCacheUpdateTimestamp > CACHE_VALIDITY_LIMIT) {
                updateDataSet()
            }
            return field
        }
        set(value) {
            field = value
            notifySubscribers()
        }
    var isLoading = false
        set(value) {
            field = value
            notifySubscribers()
        }
    var isSortedByTitle = storageManager.isSortedByTitle
        set(value) {
            field = value
            storageManager.isSortedByTitle = value
            notifySubscribers()
        }
    var shouldHideExplicit = storageManager.shouldHideExplicit
        set(value) {
            field = value
            storageManager.shouldHideExplicit = value
            notifySubscribers()
        }
    var cloudQuery = ""
        set(value) {
            field = value
            notifySubscribers()
        }
    var downloadsQuery = ""
        set(value) {
            field = value
            notifySubscribers()
        }

    fun subscribe(subscriber: Subscriber) {
        if (!subscribers.contains(subscriber)) {
            subscribers.add(subscriber)
        }
        subscriber.onUpdate()
    }

    fun unsubscribe(subscriber: Subscriber) {
        if (subscribers.contains(subscriber)) {
            subscribers.remove(subscriber)
        }
    }

    fun updateDataSet(onError: () -> Unit = {}) {
        isLoading = true
        networkManager.service.getLibrary().enqueueCall(
            onSuccess = {
                isLoading = false
                dataSet = it
                storageManager.cloudCache = dataSet
                storageManager.lastCacheUpdateTimestamp = System.currentTimeMillis()
            },
            onFailure = {
                isLoading = false
                onError()
            })
    }

    fun getCloudSongs() = dataSet.filterExplicit().sort().toList()

    fun getDownloadsSongs() = storageManager.downloads.mapNotNull { id -> dataSet.find { id == it.id } }.filterExplicit().sort()

    fun isSongDownloads(id: String) = storageManager.downloads.contains(id)

    fun addSongToDownloads(id: String) {
        if (!isSongDownloads(id)) {
            storageManager.downloads = storageManager.downloads.toMutableList().apply { add(id) }
            notifySubscribers()
        }
    }

    fun removeSongFromDownloads(id: String) {
        removeSongFromFavorites(id)
        if (isSongDownloads(id)) {
            storageManager.downloads = storageManager.downloads.toMutableList().apply { remove(id) }
            notifySubscribers()
        }
    }

    fun getFavoriteSongs() = storageManager.favorites.mapNotNull { id -> dataSet.find { id == it.id } }.filterExplicit()

    fun isSongFavorite(id: String) = storageManager.favorites.contains(id)

    fun addSongToFavorites(id: String, position: Int? = null) {
        if (!isSongFavorite(id)) {
            storageManager.favorites = storageManager.favorites.toMutableList().apply {
                if (position == null) add(id) else add(position, id)
            }
            notifySubscribers()
        }
    }

    fun removeSongFromFavorites(id: String) {
        if (isSongFavorite(id)) {
            storageManager.favorites = storageManager.favorites.toMutableList().apply { remove(id) }
            notifySubscribers()
        }
    }

    fun setFavorites(songIds: List<String>) {
        storageManager.favorites = songIds
        notifySubscribers()
    }

    fun shuffleFavorites() {
        val list = storageManager.favorites.toMutableList()
        if (list.size > SHUFFLE_LIMIT) {
            val newList = list.toMutableList()
            while (newList == list) {
                Collections.shuffle(newList)
            }
            storageManager.favorites = newList
            notifySubscribers()
        }
    }

    private fun notifySubscribers() = subscribers.forEach { it.onUpdate() }

    private fun List<SongInfo>.filterExplicit() = if (shouldHideExplicit) filter { !it.isExplicit } else this

    //TODO: Handle special characters
    private fun List<SongInfo>.sort() = sortedBy { if (isSortedByTitle) it.title else it.artist }

    companion object {
        const val SHUFFLE_LIMIT = 2
        private const val CACHE_VALIDITY_LIMIT = 1000 * 60 * 60 * 24
    }
}