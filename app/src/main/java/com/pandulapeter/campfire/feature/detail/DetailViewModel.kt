package com.pandulapeter.campfire.feature.detail

import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.databinding.ObservableInt
import android.support.v4.app.FragmentManager
import com.pandulapeter.campfire.data.model.Playlist
import com.pandulapeter.campfire.data.repository.DownloadedSongRepository
import com.pandulapeter.campfire.data.repository.HistoryRepository
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.data.repository.SongInfoRepository
import com.pandulapeter.campfire.data.repository.shared.Subscriber
import com.pandulapeter.campfire.data.repository.shared.UpdateType
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.feature.shared.dialog.PlaylistChooserBottomSheetFragment
import com.pandulapeter.campfire.networking.AnalyticsManager
import com.pandulapeter.campfire.util.toggle

/**
 * Handles events and logic for [DetailFragment].
 */
class DetailViewModel(
    songId: String,
    playlistId: Int,
    analyticsManager: AnalyticsManager,
    private val downloadedSongRepository: DownloadedSongRepository,
    private val fragmentManager: FragmentManager,
    private val playlistRepository: PlaylistRepository,
    private val songInfoRepository: SongInfoRepository,
    private val historyRepository: HistoryRepository
) : CampfireViewModel(analyticsManager), Subscriber {
    val title = ObservableField("")
    val artist = ObservableField("")
    val songIds = playlistRepository.getPlaylist(playlistId)?.songIds ?: listOf(songId)
    val adapter = SongPagerAdapter(fragmentManager, songIds)
    val shouldNavigateBack = ObservableBoolean()
    val isSongOnAnyPlaylist = ObservableBoolean()
    val shouldShowSongOptions = ObservableBoolean()
    val shouldShowPlaylistAction = playlistId == DetailFragment.NO_PLAYLIST
    val youTubeSearchQuery = ObservableField<String>()
    val shouldAllowToolbarScrolling = ObservableBoolean()
    val shouldShowAutoPlayButton = ObservableBoolean()
    val isAutoScrollStarted = ObservableBoolean()
    val autoScrollSpeed = ObservableInt(4) //TODO: Persist this value per song.
    private var selectedPosition = songIds.indexOf(songId)

    init {
        updateToolbar()
    }

    override fun onUpdate(updateType: UpdateType) {
        when (updateType) {
            is UpdateType.PlaylistsUpdated -> updatePlaylistActionIcon()
            is UpdateType.SongRemovedFromPlaylist -> if (updateType.songId == getSelectedSongId()) updatePlaylistActionIcon()
            is UpdateType.SongAddedToPlaylist -> if (updateType.songId == getSelectedSongId()) updatePlaylistActionIcon()
            is UpdateType.DownloadStarted -> if (updateType.songId == getSelectedSongId()) {
                shouldAllowToolbarScrolling.set(false)
                shouldShowAutoPlayButton.set(false)
            }
            is UpdateType.DownloadSuccessful -> if (updateType.songId == getSelectedSongId()) {
                shouldAllowToolbarScrolling.set(true)
                shouldShowAutoPlayButton.set(true)
            }
            is UpdateType.ContentEndReached -> if (updateType.songId == getSelectedSongId()) {
                isAutoScrollStarted.set(false)
            }
        }
    }

    fun onPageSelected(position: Int) {
        selectedPosition = position
        updateToolbar()
        historyRepository.addToHistory(getSelectedSongId())
        shouldAllowToolbarScrolling.set(downloadedSongRepository.isSongDownloaded(getSelectedSongId()))
        shouldShowAutoPlayButton.set(downloadedSongRepository.isSongDownloaded(getSelectedSongId()))
    }

    fun onPlaylistActionClicked() {
        val songId = getSelectedSongId()
        if (playlistRepository.getPlaylists().size == 1) {
            if (playlistRepository.isSongInPlaylist(Playlist.FAVORITES_ID, songId)) {
                playlistRepository.removeSongFromPlaylist(Playlist.FAVORITES_ID, songId)
            } else {
                playlistRepository.addSongToPlaylist(Playlist.FAVORITES_ID, songId)
            }
        } else {
            isAutoScrollStarted.set(false)
            PlaylistChooserBottomSheetFragment.show(fragmentManager, songId)
        }
    }

    fun navigateBack() {
        isAutoScrollStarted.set(false)
        shouldShowAutoPlayButton.set(false)
        shouldNavigateBack.set(true)
    }

    fun showSongOptions() = shouldShowSongOptions.set(true)

    fun onPlayOnYouTubeClicked() {
        songInfoRepository.getSongInfo(getSelectedSongId())?.let {
            youTubeSearchQuery.set("${it.artist} - ${it.title}")
        }
    }

    //TODO: Users should not be able to interrupt the animation.
    fun onAutoPlayButtonClicked() = isAutoScrollStarted.toggle()

    fun getSelectedSongId() = songIds[selectedPosition]

    private fun updateToolbar() {
        songInfoRepository.getSongInfo(getSelectedSongId())?.let {
            title.set(it.title)
            artist.set(it.artist)
            updatePlaylistActionIcon()
        }
    }

    private fun updatePlaylistActionIcon() {
        isSongOnAnyPlaylist.set(playlistRepository.isSongInAnyPlaylist(getSelectedSongId()))
    }
}