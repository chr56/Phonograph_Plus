/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.annotation.SuppressLint
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.adapter.display.GenreDisplayAdapter
import player.phonograph.mediastore.GenreLoader
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Genre

class GenrePage : AbsDisplayPage<Genre, DisplayAdapter<Genre>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Genre> {
        return GenreDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            ArrayList(), // empty until Genre loaded
            R.layout.item_list_no_image
        ) {
            showSectionName = true
        }
    }

    override fun loadDataSet() {
        loaderCoroutineScope.launch {
            val temp = GenreLoader.getAllGenres(App.instance)
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

    override fun getDataSet(): List<Genre> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun setupSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: ListOptionsPopup
    ) {

        val currentSortMode = displayUtil.sortMode
        if (DEBUG) Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable = arrayOf(SortRef.DISPLAY_NAME, SortRef.SONG_COUNT)
    }

    override fun saveSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: ListOptionsPopup,
    ) {
        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayUtil.sortMode != selected) {
            displayUtil.sortMode = selected
            loadDataSet()
            Log.d(TAG, "Write cfg: sortMode $selected")
        }
    }

    override fun getHeaderText(): CharSequence {
        return "${hostFragment.mainActivity.getString(R.string.genres)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "GenrePage"
    }
}
