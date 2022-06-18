/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.display.AlbumDisplayAdapter
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Album

class AlbumPage : AbsDisplayPage<Album, DisplayAdapter<Album>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Album> {
        val displayUtil = DisplayUtil(this)

        val layoutRes =
            if (displayUtil.gridSize > displayUtil.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG, "layoutRes: ${ if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN" }"
        )

        return AlbumDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment,
            ArrayList(), // empty until Albums loaded
            layoutRes
        ) {
            usePalette = displayUtil.colorFooter
        }
    }

    override fun loadDataSet() {
        loaderCoroutineScope.launch {
            val temp = AlbumLoader.getAllAlbums(App.instance)
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

    override fun getDataSet(): List<Album> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun setupSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: PopupWindowMainBinding
    ) {

        val currentSortMode = displayUtil.sortMode
        Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        popup.groupSortOrderRef.clearCheck()
        popup.sortOrderAlbum.visibility = View.VISIBLE
        popup.sortOrderArtist.visibility = View.VISIBLE
        popup.sortOrderYear.visibility = View.VISIBLE
        popup.sortOrderSongCount.visibility = View.VISIBLE
        when (currentSortMode.sortRef) {
            SortRef.ALBUM_NAME -> popup.groupSortOrderRef.check(R.id.sort_order_album)
            SortRef.ARTIST_NAME -> popup.groupSortOrderRef.check(R.id.sort_order_artist)
            SortRef.YEAR -> popup.groupSortOrderRef.check(R.id.sort_order_year)
            SortRef.SONG_COUNT -> popup.groupSortOrderRef.check(R.id.sort_order_song_count)
            else -> popup.groupSortOrderRef.clearCheck()
        }

        when (currentSortMode.revert) {
            false -> popup.groupSortOrderMethod.check(R.id.sort_method_a_z)
            true -> popup.groupSortOrderMethod.check(R.id.sort_method_z_a)
        }
    }

    override fun saveSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: PopupWindowMainBinding
    ) {

        // sort order
        val revert = when (popup.groupSortOrderMethod.checkedRadioButtonId) {
            R.id.sort_method_z_a -> true
            R.id.sort_method_a_z -> false
            else -> false
        }
        val sortRef = when (popup.groupSortOrderRef.checkedRadioButtonId) {
            R.id.sort_order_album -> SortRef.ALBUM_NAME
            R.id.sort_order_year -> SortRef.YEAR
            R.id.sort_order_artist -> SortRef.ARTIST_NAME
            R.id.sort_order_song_count -> SortRef.SONG_COUNT
            else -> SortRef.ID
        }
        val selected = SortMode(sortRef, revert)
        if (displayUtil.sortMode != selected) {
            displayUtil.sortMode = selected
            loadDataSet()
            Log.d(TAG, "Write cfg: sortMode $selected")
        }
    }

    override fun getHeaderText(): CharSequence {
        return "${hostFragment.mainActivity.getString(R.string.albums)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "AlbumPage"
    }
}
