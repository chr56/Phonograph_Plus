/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.home

import android.annotation.SuppressLint
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.adapter.display.ArtistDisplayAdapter
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.Artist

class ArtistPage : AbsDisplayPage<Artist, DisplayAdapter<Artist>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Artist> {
        val displayUtil = DisplayUtil(this)

        val layoutRes =
            if (displayUtil.gridSize > displayUtil.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG, "layoutRes: ${ if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN" }"
        )

        return ArtistDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            ArrayList(), // empty until Artist loaded
            layoutRes
        ) {
            usePalette = displayUtil.colorFooter
        }
    }

    override fun loadDataSet() {
        loaderCoroutineScope.launch {
            val temp = ArtistLoader.getAllArtists(App.instance)
            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataset = temp
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshDataSet() {
        adapter.notifyDataSetChanged()
    }

    override fun getDataSet(): List<Artist> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun setupSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: ListOptionsPopup
    ) {

        val currentSortMode = displayUtil.sortMode
        if (BuildConfig.DEBUG) Log.d(GenrePage.TAG, "Read cfg: sortMode $currentSortMode")

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable = arrayOf(SortRef.ARTIST_NAME, SortRef.ALBUM_COUNT, SortRef.SONG_COUNT)
    }

    override fun saveSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: ListOptionsPopup
    ) {

        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayUtil.sortMode != selected) {
            displayUtil.sortMode = selected
            loadDataSet()
            Log.d(TAG, "Write cfg: sortMode $selected")
        }
    }

    override fun getHeaderText(): CharSequence {
        val n = getDataSet().size
        return hostFragment.mainActivity.resources.getQuantityString(R.plurals.x_artists, n, n)
    }

    companion object {
        const val TAG = "ArtistPage"
    }
}
