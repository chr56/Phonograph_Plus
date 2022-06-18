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
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.mediastore.MediaStoreUtil
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Song

class SongPage : AbsDisplayPage<Song, DisplayAdapter<Song>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Song> {
        val displayUtil = DisplayUtil(this)

        val layoutRes =
            if (displayUtil.gridSize > displayUtil.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG, "layoutRes: ${ if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN" }"
        )

        return SongDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment,
            ArrayList(), // empty until songs loaded
            layoutRes
        ) {
            usePalette = displayUtil.colorFooter
        }
    }

    override fun loadDataSet() {
        loaderCoroutineScope.launch {
            val temp = MediaStoreUtil.getAllSongs(App.instance)
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

    override fun getDataSet(): List<Song> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun setupSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: PopupWindowMainBinding
    ) {

        val currentSortMode = displayUtil.sortMode
        Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        popup.sortOrderContent.clearCheck()
        popup.sortOrderSong.visibility = View.VISIBLE
        popup.sortOrderAlbum.visibility = View.VISIBLE
        popup.sortOrderArtist.visibility = View.VISIBLE
        popup.sortOrderYear.visibility = View.VISIBLE
        popup.sortOrderDateAdded.visibility = View.VISIBLE
        popup.sortOrderDateModified.visibility = View.VISIBLE
        popup.sortOrderDuration.visibility = View.VISIBLE
        when (currentSortMode.sortRef) {
            SortRef.SONG_NAME -> popup.sortOrderContent.check(R.id.sort_order_song)
            SortRef.ALBUM_NAME -> popup.sortOrderContent.check(R.id.sort_order_album)
            SortRef.ARTIST_NAME -> popup.sortOrderContent.check(R.id.sort_order_artist)
            SortRef.YEAR -> popup.sortOrderContent.check(R.id.sort_order_year)
            SortRef.ADDED_DATE -> popup.sortOrderContent.check(R.id.sort_order_date_added)
            SortRef.MODIFIED_DATE -> popup.sortOrderContent.check(R.id.sort_order_date_modified)
            SortRef.SONG_DURATION -> popup.sortOrderContent.check(R.id.sort_order_duration)
            else -> popup.sortOrderContent.clearCheck()
        }
        when (currentSortMode.revert) {
            false -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
            true -> popup.sortOrderBasic.check(R.id.sort_order_z_a)
        }
    }

    override fun saveSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: PopupWindowMainBinding
    ) {

        // sort order

        val revert = when (popup.sortOrderBasic.checkedRadioButtonId) {
            R.id.sort_order_z_a -> true
            R.id.sort_order_a_z -> false
            else -> false
        }
        val sortRef: SortRef = when (popup.sortOrderContent.checkedRadioButtonId) {
            R.id.sort_order_song -> SortRef.SONG_NAME
            R.id.sort_order_album -> SortRef.ALBUM_NAME
            R.id.sort_order_artist -> SortRef.ARTIST_NAME
            R.id.sort_order_year -> SortRef.YEAR
            R.id.sort_order_date_added -> SortRef.ADDED_DATE
            R.id.sort_order_date_modified -> SortRef.MODIFIED_DATE
            R.id.sort_order_duration -> SortRef.SONG_DURATION
            else -> SortRef.ID
        }

        val selected = SortMode(sortRef, revert)
        if (displayUtil.sortMode != selected) {
            displayUtil.sortMode = selected
            loadDataSet()
            Log.d(AlbumPage.TAG, "Write cfg: sortMode $selected")
        }
    }

    override fun getHeaderText(): CharSequence {
        return "${hostFragment.mainActivity.getString(R.string.songs)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "SongPage"
    }
}
