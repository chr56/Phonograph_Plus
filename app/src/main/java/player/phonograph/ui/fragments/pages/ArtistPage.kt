/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.adapter.display.ArtistDisplayAdapter
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.model.Artist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import androidx.fragment.app.viewModels
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope

class ArtistPage : AbsDisplayPage<Artist, DisplayAdapter<Artist>>() {

    override val viewModel: AbsDisplayPageViewModel<Artist> get() = _viewModel

    private val _viewModel: ArtistPageViewModel by viewModels()

    class ArtistPageViewModel : AbsDisplayPageViewModel<Artist>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Artist> {
            return ArtistLoader.all(App.instance)
        }

        override val headerTextRes: Int get() = R.plurals.item_artists
    }

    override val displayConfigTarget get() = DisplayConfigTarget.ArtistPage

    override fun initAdapter(): DisplayAdapter<Artist> {
        val displayConfig = DisplayConfig(displayConfigTarget)

        val layoutRes =
            if (displayConfig.gridSize > displayConfig.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG,
            "layoutRes: ${if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN"}"
        )

        return ArtistDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            ArrayList(), // empty until Artist loaded
            layoutRes
        ) {
            usePalette = displayConfig.colorFooter
        }
    }


    override fun updateDataset(dataSet: List<Artist>) {
        adapter.dataset = dataSet
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshDataSet() {
        adapter.notifyDataSetChanged()
    }

    override fun setupSortOrderImpl(
        displayConfig: DisplayConfig,
        popup: ListOptionsPopup,
    ) {

        val currentSortMode = displayConfig.sortMode
        if (BuildConfig.DEBUG) Log.d(GenrePage.TAG, "Read cfg: sortMode $currentSortMode")

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable = arrayOf(SortRef.ARTIST_NAME, SortRef.ALBUM_COUNT, SortRef.SONG_COUNT)
    }

    override fun saveSortOrderImpl(
        displayConfig: DisplayConfig,
        popup: ListOptionsPopup,
    ) {

        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayConfig.sortMode != selected) {
            displayConfig.sortMode = selected
            viewModel.loadDataset(requireContext())
            Log.d(TAG, "Write cfg: sortMode $selected")
        }
    }


    companion object {
        const val TAG = "ArtistPage"
    }
}
