package com.pandulapeter.campfire.feature.home.library

import android.content.Context
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.model.local.Language
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.feature.home.shared.SongListViewModel
import com.pandulapeter.campfire.feature.home.shared.SongViewModel
import com.pandulapeter.campfire.feature.shared.widget.ToolbarTextInputView
import com.pandulapeter.campfire.util.normalize
import com.pandulapeter.campfire.util.onTextChanged
import com.pandulapeter.campfire.util.swap
import org.koin.android.ext.android.inject

class LibraryViewModel(
    context: Context,
    val toolbarTextInputView: ToolbarTextInputView,
    private val updateSearchToggleDrawable: (Boolean) -> Unit,
    private val onDataLoaded: (languages: List<Language>) -> Unit,
    private val showFilters: () -> Unit
) : SongListViewModel() {

    private val newString = context.getString(R.string.new_tag)
    private val popularString = context.getString(R.string.popular_tag)
    private val preferenceDatabase by inject<PreferenceDatabase>()
    var query = ""
        set(value) {
            if (field != value) {
                field = value
                updateAdapterItems(true)
            }
        }
    var shouldShowDownloadedOnly = preferenceDatabase.shouldShowDownloadedOnly
        set(value) {
            if (field != value) {
                field = value
                preferenceDatabase.shouldShowDownloadedOnly = value
                updateAdapterItems()
            }
        }
    var shouldShowWorkInProgress = preferenceDatabase.shouldShowWorkInProgress
        set(value) {
            if (field != value) {
                field = value
                preferenceDatabase.shouldShowWorkInProgress = value
                updateAdapterItems()
            }
        }
    var shouldShowExplicit = preferenceDatabase.shouldShowExplicit
        set(value) {
            if (field != value) {
                field = value
                preferenceDatabase.shouldShowExplicit = value
                updateAdapterItems()
            }
        }
    var sortingMode = SortingMode.fromIntValue(preferenceDatabase.sortingMode)
        set(value) {
            if (field != value) {
                field = value
                preferenceDatabase.sortingMode = value.intValue
                updateAdapterItems(true)
            }
        }
    var disabledLanguageFilters = preferenceDatabase.disabledLanguageFilters
        set(value) {
            if (field != value) {
                field = value
                preferenceDatabase.disabledLanguageFilters = value
                updateAdapterItems(true)
            }
        }
    var languages = mutableListOf<Language>()

    init {
        toolbarTextInputView.apply {
            textInput.onTextChanged { if (isTextInputVisible) query = it }
        }
    }

    override fun onSongRepositoryDataUpdated(data: List<Song>) {
        super.onSongRepositoryDataUpdated(data)
        if (data.isNotEmpty()) {
            languages.swap(data
                .map {
                    when (it.language) {
                        Language.SupportedLanguages.ENGLISH.id -> Language.Known.English
                        Language.SupportedLanguages.HUNGARIAN.id -> Language.Known.Hungarian
                        Language.SupportedLanguages.ROMANIAN.id -> Language.Known.Romanian
                        else -> Language.Unknown
                    }
                }
                .sortedBy { it.nameResource }
                .distinct()
            )
            onDataLoaded(languages)
        }
    }

    override fun onListUpdated(items: List<SongViewModel>) {
        super.onListUpdated(items)
        if (librarySongs.toList().isNotEmpty()) {
            updatePlaceholder()
            buttonText.set(R.string.library_filters)
        }
    }

    override fun onActionButtonClicked() = when {
        librarySongs.toList().isEmpty() -> updateData()
        else -> showFilters()
    }

    override fun Sequence<Song>.createViewModels() = filterByQuery()
        .filterDownloaded()
        .filterByLanguage()
        .filterWorkInProgress()
        .filterExplicit()
        .sort()
        .map { SongViewModel(songDetailRepository, it) }
        .toList()

    fun toggleTextInputVisibility() {
        toolbarTextInputView.run {
            if (title.tag == null) {
                val shouldScrollToTop = !query.isEmpty()
                animateTextInputVisibility(!isTextInputVisible)
                if (isTextInputVisible) {
                    textInput.setText("")
                }
                updateSearchToggleDrawable(toolbarTextInputView.isTextInputVisible)
                updateAdapterItems(!isTextInputVisible && shouldScrollToTop)
            }
        }
    }

    fun isHeader(position: Int) = when (sortingMode) {
        SortingMode.TITLE -> position == 0 || adapter.items[position].song.getNormalizedTitle()[0] != adapter.items[position - 1].song.getNormalizedTitle()[0]
        SortingMode.ARTIST -> position == 0 || adapter.items[position].song.getNormalizedArtist()[0] != adapter.items[position - 1].song.getNormalizedArtist()[0]
        SortingMode.POPULARITY -> adapter.items[0].song.isNew && (position == 0 || adapter.items[position].song.isNew != adapter.items[position - 1].song.isNew)
    }

    fun getHeaderTitle(position: Int) = when (sortingMode) {
        SortingMode.TITLE -> adapter.items[position].song.getNormalizedTitle()[0].toString().toUpperCase()
        SortingMode.ARTIST -> adapter.items[position].song.getNormalizedArtist()[0].toString().toUpperCase()
        SortingMode.POPULARITY -> if (adapter.items[position].song.isNew) newString else popularString
    }

    //TODO: Prioritize results that begin with the searchQuery.
    private fun Sequence<Song>.filterByQuery() = if (toolbarTextInputView.isTextInputVisible) {
        query.trim().normalize().let { query ->
            filter { it.getNormalizedTitle().contains(query, true) || it.getNormalizedArtist().contains(query, true) }
        }
    } else this

    private fun Sequence<Song>.filterDownloaded() = if (shouldShowDownloadedOnly) filter { songDetailRepository.isSongDownloaded(it.id) } else this

    private fun Sequence<Song>.filterByLanguage() = filter { !disabledLanguageFilters.contains(it.language ?: Language.Unknown.id) }

    private fun Sequence<Song>.filterWorkInProgress() = if (!shouldShowWorkInProgress) filter { it.version ?: 0 >= 0 } else this

    private fun Sequence<Song>.filterExplicit() = if (!shouldShowExplicit) filter { it.isExplicit != true } else this

    private fun Sequence<Song>.sort() = when (sortingMode) {
        SortingMode.TITLE -> sortedBy { it.getNormalizedArtist() }.sortedBy { it.getNormalizedTitle() }
        SortingMode.ARTIST -> sortedBy { it.getNormalizedTitle() }.sortedBy { it.getNormalizedArtist() }
        SortingMode.POPULARITY -> sortedBy { it.getNormalizedArtist() }.sortedBy { it.getNormalizedTitle() }.sortedByDescending { it.popularity }.sortedByDescending { it.isNew }
    }

    private fun updatePlaceholder() = placeholderText.set(
        when {
            toolbarTextInputView.isTextInputVisible && query.isNotEmpty() -> R.string.library_placeholder_search
            else -> R.string.library_placeholder_filters
        }
    )

    enum class SortingMode(val intValue: Int) {
        TITLE(0),
        ARTIST(1),
        POPULARITY(2);

        companion object {
            fun fromIntValue(value: Int) = SortingMode.values().find { it.intValue == value } ?: TITLE
        }
    }
}