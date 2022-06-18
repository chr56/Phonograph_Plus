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
import player.phonograph.adapter.display.ArtistDisplayAdapter
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
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
            hostFragment,
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
        Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        with(popup.viewBinding) {
            groupSortOrderRef.clearCheck()
            sortOrderArtist.visibility = View.VISIBLE
            sortOrderAlbumCount.visibility = View.VISIBLE
            sortOrderSongCount.visibility = View.VISIBLE
            when (currentSortMode.sortRef) {
                SortRef.ARTIST_NAME -> groupSortOrderRef.check(R.id.sort_order_artist)
                SortRef.ALBUM_COUNT -> groupSortOrderRef.check(R.id.sort_order_album_count)
                SortRef.SONG_COUNT -> groupSortOrderRef.check(R.id.sort_order_song_count)
                else -> groupSortOrderRef.clearCheck()
            }

            when (currentSortMode.revert) {
                false -> groupSortOrderMethod.check(R.id.sort_method_a_z)
                true -> groupSortOrderMethod.check(R.id.sort_method_z_a)
            }
        }
    }

    override fun saveSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: ListOptionsPopup
    ) {

        // sort order
        val revert = when (popup.viewBinding.groupSortOrderMethod.checkedRadioButtonId) {
            R.id.sort_method_z_a -> true
            R.id.sort_method_a_z -> false
            else -> false
        }
        val sortRef = when (popup.viewBinding.groupSortOrderRef.checkedRadioButtonId) {
            R.id.sort_order_artist -> SortRef.ARTIST_NAME
            R.id.sort_order_album_count -> SortRef.ALBUM_COUNT
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
        return "${hostFragment.mainActivity.getString(R.string.artists)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "ArtistPage"
    }
}
