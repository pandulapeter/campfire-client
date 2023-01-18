package com.pandulapeter.campfire.domain.implementation.useCases

import com.pandulapeter.campfire.data.repository.api.CollectionRepository
import com.pandulapeter.campfire.data.repository.api.DatabaseRepository
import com.pandulapeter.campfire.data.repository.api.PlaylistRepository
import com.pandulapeter.campfire.data.repository.api.SongRepository
import com.pandulapeter.campfire.data.repository.api.UserPreferencesRepository
import com.pandulapeter.campfire.domain.api.useCases.LoadScreenDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class LoadScreenDataUseCaseImpl internal constructor(
    private val collectionRepository: CollectionRepository,
    private val databaseRepository: DatabaseRepository,
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : LoadScreenDataUseCase {

    private val scope = object : CoroutineScope {
        override val coroutineContext = SupervisorJob() + Dispatchers.Default
    }

    override suspend operator fun invoke(isForceRefresh: Boolean) {
        with(scope) {
            listOf(
                async { playlistRepository.loadPlaylistsIfNeeded() },
                async {
                    val userPreferences = userPreferencesRepository.loadUserPreferencesIfNeeded()
                    val databaseUrls = databaseRepository.loadDatabasesIfNeeded()
                        .filter { it.isEnabled }
                        .filterNot { it.url in userPreferences.unselectedDatabaseUrls }
                        .sortedBy { it.priority }
                        .map { it.url }
                    listOf(
                        async { collectionRepository.loadCollections(databaseUrls, isForceRefresh) },
                        async { songRepository.loadSongs(databaseUrls, isForceRefresh) }
                    ).awaitAll()
                }
            ).awaitAll()
        }
    }
}