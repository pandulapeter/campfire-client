package com.pandulapeter.campfire.feature.home.shared.songList

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v4.app.SharedElementCallback
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.FragmentSongListBinding
import com.pandulapeter.campfire.feature.home.collections.detail.CollectionDetailViewModel
import com.pandulapeter.campfire.feature.home.playlist.PlaylistViewModel
import com.pandulapeter.campfire.feature.home.shared.DisableScrollLinearLayoutManager
import com.pandulapeter.campfire.feature.shared.TopLevelFragment
import com.pandulapeter.campfire.feature.shared.dialog.PlaylistChooserBottomSheetFragment
import com.pandulapeter.campfire.feature.shared.widget.StateLayout
import com.pandulapeter.campfire.util.color
import com.pandulapeter.campfire.util.hideKeyboard
import com.pandulapeter.campfire.util.onEventTriggered
import com.pandulapeter.campfire.util.onPropertyChanged


abstract class SongListFragment<out VM : SongListViewModel> : TopLevelFragment<FragmentSongListBinding, VM>(R.layout.fragment_song_list) {

    override val shouldDelaySubscribing get() = viewModel.isDetailScreenOpen
    protected lateinit var linearLayoutManager: DisableScrollLinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                val index = viewModel.adapter.items.indexOfFirst { it is SongListItemViewModel.SongViewModel && it.song.id == mainActivity.lastSongId }
                if (index != RecyclerView.NO_POSITION) {
                    (binding.recyclerView.findViewHolderForAdapterPosition(index)
                            ?: binding.recyclerView.findViewHolderForAdapterPosition(linearLayoutManager.findLastVisibleItemPosition()))?.let {
                        sharedElements[names[0]] = it.itemView
                    }
                }
            }
        })
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)
        viewModel.adapter.run {
            itemClickListener = { position, clickedView ->
                if (linearLayoutManager.isScrollEnabled && !mainActivity.isUiBlocked) {
                    (items[position] as? SongListItemViewModel.SongViewModel)?.let {
                        if (items.size > 1) {
                            linearLayoutManager.isScrollEnabled = false
                            viewModel.isDetailScreenOpen = true
                        }
                        mainActivity.isUiBlocked = true
                        onDetailScreenOpened()
                        val shouldSendMultipleSongs = viewModel is PlaylistViewModel || viewModel is CollectionDetailViewModel
                        mainActivity.openDetailScreen(
                            clickedView,
                            if (shouldSendMultipleSongs) items.filterIsInstance<SongListItemViewModel.SongViewModel>().map { it.song } else listOf(it.song),
                            items.size > 1,
                            if (shouldSendMultipleSongs) position else 0,
                            viewModel !is PlaylistViewModel
                        )
                    }
                }
            }
            downloadActionClickListener = { position ->
                if (linearLayoutManager.isScrollEnabled && !mainActivity.isUiBlocked) {
                    (items[position] as? SongListItemViewModel.SongViewModel)?.let { viewModel.downloadSong(it.song) }
                }
            }
            playlistActionClickListener = { position ->
                if (linearLayoutManager.isScrollEnabled && !mainActivity.isUiBlocked) {
                    (items[position] as? SongListItemViewModel.SongViewModel)?.let {
                        if (viewModel.areThereMoreThanOnePlaylists()) {
                            PlaylistChooserBottomSheetFragment.show(childFragmentManager, it.song.id, viewModel.screenName)
                        } else {
                            viewModel.toggleFavoritesState(it.song.id)
                        }
                    }
                }
            }
        }
        viewModel.shouldShowUpdateErrorSnackbar.onEventTriggered(this) {
            showSnackbar(
                message = R.string.library_update_error,
                action = { viewModel.updateData() })
        }
        viewModel.downloadSongError.onEventTriggered(this) { song ->
            song?.let {
                binding.root.post {
                    if (isAdded) {
                        showSnackbar(
                            message = mainActivity.getString(R.string.library_song_download_error, song.title),
                            action = { viewModel.downloadSong(song) })
                    }
                }
            }
        }
        viewModel.isLoading.onPropertyChanged(this) {
            if (viewModel.state.get() == StateLayout.State.NORMAL) {
                binding.swipeRefreshLayout.isRefreshing = it
            }
        }
        binding.swipeRefreshLayout.run {
            setOnRefreshListener { viewModel.updateData() }
            setColorSchemeColors(context.color(R.color.accent))
        }
        linearLayoutManager = DisableScrollLinearLayoutManager(mainActivity)
        binding.recyclerView.run {
            layoutManager = linearLayoutManager
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && !recyclerView.isAnimating) {
                        hideKeyboard(activity?.currentFocus)
                    }
                }
            })
        }
        binding.recyclerView.addOnLayoutChangeListener(
            object : OnLayoutChangeListener {
                override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                    binding.recyclerView.removeOnLayoutChangeListener(this)
                    if (reenterTransition != null) {
                        val index = viewModel.adapter.items.indexOfFirst { it is SongListItemViewModel.SongViewModel && it.song.id == mainActivity.lastSongId }
                        if (index != RecyclerView.NO_POSITION) {
                            val viewAtPosition = linearLayoutManager.findViewByPosition(index)
                            if (viewAtPosition == null || linearLayoutManager.isViewPartiallyVisible(viewAtPosition, false, true)) {
                                linearLayoutManager.isScrollEnabled = true
                                binding.recyclerView.run { post { if (isAdded) scrollToPosition(index) } }
                            }
                        }
                    }
                }
            })
        (view.parent as? ViewGroup)?.run {
            viewTreeObserver?.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewTreeObserver?.removeOnPreDrawListener(this)
                    startPostponedEnterTransition()
                    return true
                }
            })
            requestLayout()
        }
    }

    override fun onBackPressed() = !linearLayoutManager.isScrollEnabled

    override fun updateUI() {
        super.updateUI()
        linearLayoutManager.isScrollEnabled = true
    }

    protected open fun onDetailScreenOpened() = Unit

    protected fun shuffleSongs(source: String) {
        val tempList = viewModel.adapter.items.filterIsInstance<SongListItemViewModel.SongViewModel>().map { it.song }.toMutableList()
        tempList.shuffle()
        analyticsManager.onShuffleButtonPressed(source, tempList.size)
        mainActivity.openDetailScreen(null, tempList, false, 0, viewModel is CollectionDetailViewModel)
    }
}