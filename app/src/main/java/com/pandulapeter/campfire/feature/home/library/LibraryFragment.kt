package com.pandulapeter.campfire.feature.home.library

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.ViewSearchControlsBinding
import com.pandulapeter.campfire.feature.home.shared.songList.SongListFragment
import com.pandulapeter.campfire.feature.shared.widget.ToolbarButton
import com.pandulapeter.campfire.feature.shared.widget.ToolbarTextInputView
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.util.*


class LibraryFragment : SongListFragment<LibraryViewModel>() {

    companion object {
        private const val COMPOUND_BUTTON_LONG_TRANSITION_DELAY = 300L
    }

    override val canScrollToolbar get() = !viewModel.toolbarTextInputView.isTextInputVisible && binding.recyclerView.canScroll()
    override val viewModel: LibraryViewModel by lazy {
        LibraryViewModel(
            context = getCampfireActivity(),
            toolbarTextInputView = ToolbarTextInputView(
                getCampfireActivity().toolbarContext,
                R.string.library_search,
                true
            ).apply { title.updateToolbarTitle(R.string.home_library) },
            updateSearchToggleDrawable = {
                searchToggle.setImageDrawable((if (it) drawableSearchToClose else drawableCloseToSearch).apply { (this as? AnimatedVectorDrawableCompat)?.start() })
                getCampfireActivity().transitionMode = true
                binding.swipeRefreshLayout.isEnabled = !it
                binding.swipeRefreshLayout.isRefreshing = viewModel.isLoading.get()
                binding.root.post {
                    if (isAdded) {
                        searchControlsViewModel.isVisible.set(it)
                    }
                }
            },
            onDataLoaded = { languages ->
                getCampfireActivity().updateAppBarView(searchControlsBinding.root)
                getCampfireActivity().enableSecondaryNavigationDrawer(R.menu.library)
                initializeCompoundButton(R.id.downloaded_only) { viewModel.shouldShowDownloadedOnly }
                initializeCompoundButton(R.id.show_explicit) { viewModel.shouldShowExplicit }
                initializeCompoundButton(R.id.sort_by_title) { viewModel.sortingMode == LibraryViewModel.SortingMode.TITLE }
                initializeCompoundButton(R.id.sort_by_artist) { viewModel.sortingMode == LibraryViewModel.SortingMode.ARTIST }
                initializeCompoundButton(R.id.sort_by_popularity) { viewModel.sortingMode == LibraryViewModel.SortingMode.POPULARITY }
                getCampfireActivity().secondaryNavigationMenu.findItem(R.id.filter_by_language).subMenu.run {
                    clear()
                    languages.forEachIndexed { index, language ->
                        add(R.id.language_container, language.nameResource, index, language.nameResource).apply {
                            setActionView(R.layout.widget_checkbox)
                            initializeCompoundButton(language.nameResource) { !viewModel.disabledLanguageFilters.contains(language.id) }
                        }
                    }
                }
                getCampfireActivity().toolbarContext.let { context ->
                    getCampfireActivity().updateToolbarButtons(
                        listOf(
                            eraseButton,
                            searchToggle,
                            context.createToolbarButton(R.drawable.ic_filter_and_sort_24dp) { getCampfireActivity().openSecondaryNavigationDrawer() }
                        ))
                }
            },
            openSecondaryNavigationDrawer = { getCampfireActivity().openSecondaryNavigationDrawer() }
        )
    }
    private var Bundle.isTextInputVisible by BundleArgumentDelegate.Boolean("isTextInputVisible")
    private var Bundle.searchQuery by BundleArgumentDelegate.String("searchQuery")
    private var Bundle.placeholderText by BundleArgumentDelegate.Int("placeholderText")
    private var Bundle.buttonText by BundleArgumentDelegate.Int("buttonText")
    private var Bundle.buttonIcon by BundleArgumentDelegate.Int("buttonIcon")
    private val searchToggle: ToolbarButton by lazy {
        getCampfireActivity().toolbarContext.createToolbarButton(R.drawable.ic_search_24dp) {
            viewModel.toggleTextInputVisibility()
        }
    }
    private val eraseButton: ToolbarButton by lazy {
        getCampfireActivity().toolbarContext.createToolbarButton(R.drawable.ic_eraser_24dp) {
            viewModel.toolbarTextInputView.textInput.setText("")
        }.apply {
            scaleX = 0f
            scaleY = 0f
            alpha = 0.5f
            isEnabled = false
        }
    }
    private val drawableCloseToSearch by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getCampfireActivity().animatedDrawable(R.drawable.avd_close_to_search_24dp) else getCampfireActivity().drawable(R.drawable.ic_search_24dp)
    }
    private val drawableSearchToClose by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getCampfireActivity().animatedDrawable(R.drawable.avd_search_to_close_24dp) else getCampfireActivity().drawable(R.drawable.ic_close_24dp)
    }
    private val searchControlsViewModel = SearchControlsViewModel()
    private val searchControlsBinding by lazy {
        DataBindingUtil.inflate<ViewSearchControlsBinding>(LayoutInflater.from(getCampfireActivity().toolbarContext), R.layout.view_search_controls, null, false).apply {
            viewModel = searchControlsViewModel
            executePendingBindings()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyticsManager.onTopLevelScreenOpened(AnalyticsManager.PARAM_VALUE_SCREEN_LIBRARY)
        savedInstanceState?.let {
            searchControlsViewModel.isVisible.set(savedInstanceState.isTextInputVisible)
            if (it.isTextInputVisible) {
                searchToggle.setImageDrawable(getCampfireActivity().drawable(R.drawable.ic_close_24dp))
                viewModel.toolbarTextInputView.textInput.run {
                    setText(savedInstanceState.searchQuery)
                    setSelection(text.length)
                    viewModel.query = text.toString()
                }
                viewModel.toolbarTextInputView.showTextInput()
            }
            viewModel.placeholderText.set(savedInstanceState.placeholderText)
            viewModel.buttonText.set(savedInstanceState.buttonText)
            viewModel.buttonIcon.set(savedInstanceState.buttonIcon)
        }
        viewModel.shouldShowEraseButton.onPropertyChanged { eraseButton.animate().scaleX(if (it) 1f else 0f).scaleY(if (it) 1f else 0f).start() }
        viewModel.shouldEnableEraseButton.onPropertyChanged {
            eraseButton.animate().alpha(if (it) 1f else 0.5f).start()
            eraseButton.isEnabled = it
        }
        viewModel.toolbarTextInputView.textInput.requestFocus()
        searchControlsViewModel.searchInTitles.onPropertyChanged(this) {
            binding.root.postDelayed(
                { if (isAdded) viewModel.shouldSearchInTitles = it },
                COMPOUND_BUTTON_LONG_TRANSITION_DELAY
            )
        }
        searchControlsViewModel.searchInArtists.onPropertyChanged(this) {
            binding.root.postDelayed(
                { if (isAdded) viewModel.shouldSearchInArtists = it },
                COMPOUND_BUTTON_LONG_TRANSITION_DELAY
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.restoreToolbarButtons()
    }

    override fun onPause() {
        super.onPause()
        toolbarWidth = viewModel.toolbarTextInputView.width
    }

    override fun onSaveInstanceState(outState: Bundle) = outState.run {
        super.onSaveInstanceState(this)
        isTextInputVisible = viewModel.toolbarTextInputView.isTextInputVisible
        searchQuery = viewModel.query
        placeholderText = viewModel.placeholderText.get()
        buttonText = viewModel.buttonText.get()
        buttonIcon = viewModel.buttonIcon.get()
    }

    override fun onNavigationItemSelected(menuItem: MenuItem) = viewModel.run {
        when (menuItem.itemId) {
            R.id.downloaded_only -> consumeAndUpdateBoolean(menuItem, {
                analyticsManager.onLibraryFilterToggled(AnalyticsManager.PARAM_VALUE_FILTER_DOWNLOADED_ONLY, it)
                shouldShowDownloadedOnly = it
            }, { shouldShowDownloadedOnly })
            R.id.show_explicit -> consumeAndUpdateBoolean(menuItem, {
                analyticsManager.onLibraryFilterToggled(AnalyticsManager.PARAM_VALUE_FILTER_SHOW_EXPLICIT, it)
                shouldShowExplicit = it
            }, { shouldShowExplicit })
            R.id.sort_by_title -> consumeAndUpdateSortingMode(LibraryViewModel.SortingMode.TITLE) {
                analyticsManager.onLibrarySortingModeUpdated(AnalyticsManager.PARAM_VALUE_BY_TITLE)
                sortingMode = it
            }
            R.id.sort_by_artist -> consumeAndUpdateSortingMode(LibraryViewModel.SortingMode.ARTIST) {
                analyticsManager.onLibrarySortingModeUpdated(AnalyticsManager.PARAM_VALUE_BY_ARTIST)
                sortingMode = it
            }
            R.id.sort_by_popularity -> consumeAndUpdateSortingMode(LibraryViewModel.SortingMode.POPULARITY) {
                analyticsManager.onLibrarySortingModeUpdated(AnalyticsManager.PARAM_VALUE_BY_POPULARITY)
                sortingMode = it
            }
            else -> consumeAndUpdateLanguageFilter(menuItem, viewModel.languages.find { it.nameResource == menuItem.itemId }?.id ?: "")
        }
    }

    override fun inflateToolbarTitle(context: Context) = viewModel.toolbarTextInputView

    override fun onBackPressed() = if (viewModel.toolbarTextInputView.isTextInputVisible) {
        viewModel.toggleTextInputVisibility()
        true
    } else super.onBackPressed()

    override fun onDetailScreenOpened() {
        if (viewModel.toolbarTextInputView.isTextInputVisible && viewModel.query.trim().isEmpty()) {
            viewModel.toggleTextInputVisibility()
        }
    }

    private fun consumeAndUpdateLanguageFilter(menuItem: MenuItem, languageId: String) = consume {
        viewModel.disabledLanguageFilters.run {
            viewModel.disabledLanguageFilters = toMutableSet().apply { if (contains(languageId)) remove(languageId) else add(languageId) }
            analyticsManager.onLibraryFilterToggled(AnalyticsManager.PARAM_VALUE_FILTER_LANGUAGE + languageId, contains(languageId))
            (menuItem.actionView as? CompoundButton).updateCheckedStateWithDelay(contains(languageId))
        }
    }

    private inline fun consumeAndUpdateSortingMode(sortingMode: LibraryViewModel.SortingMode, crossinline setValue: (LibraryViewModel.SortingMode) -> Unit) = consume {
        setValue(sortingMode)
        (getCampfireActivity().secondaryNavigationMenu[R.id.sort_by_title].actionView as? CompoundButton).updateCheckedStateWithDelay(sortingMode == LibraryViewModel.SortingMode.TITLE)
        (getCampfireActivity().secondaryNavigationMenu[R.id.sort_by_artist].actionView as? CompoundButton).updateCheckedStateWithDelay(sortingMode == LibraryViewModel.SortingMode.ARTIST)
        (getCampfireActivity().secondaryNavigationMenu[R.id.sort_by_popularity].actionView as? CompoundButton)?.updateCheckedStateWithDelay(sortingMode == LibraryViewModel.SortingMode.POPULARITY)
    }

    private operator fun Menu.get(@IdRes id: Int) = findItem(id)
}