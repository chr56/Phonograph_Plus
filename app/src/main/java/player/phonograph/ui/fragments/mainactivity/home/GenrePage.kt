/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.util.Log
import android.view.View
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.adapter.display.GenreDisplayAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.mediastore.GenreLoader
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Genre
import player.phonograph.util.Util

class GenrePage : AbsDisplayPage<Genre, DisplayAdapter<Genre>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Genre> {
        return GenreDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment,
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

    override fun getDataSet(): List<Genre> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding) {
        val displayUtil = DisplayUtil(this)

        // grid size
        popup.textGridSize.visibility = View.VISIBLE
        popup.gridSize.visibility = View.VISIBLE
        if (Util.isLandscape(resources)) popup.textGridSize.text =
            resources.getText(R.string.action_grid_size_land)
        val current = displayUtil.gridSize
        val max = displayUtil.maxGridSize
        for (i in 0 until max) popup.gridSize.getChildAt(i).visibility = View.VISIBLE
        popup.gridSize.clearCheck()
        (popup.gridSize.getChildAt(current - 1) as RadioButton).isChecked = true

        // sort order
        popup.sortOrderBasic.visibility = View.VISIBLE
        popup.textSortOrderBasic.visibility = View.VISIBLE
        popup.sortOrderContent.visibility = View.VISIBLE
        popup.textSortOrderContent.visibility = View.VISIBLE
        for (i in 0 until popup.sortOrderContent.childCount) popup.sortOrderContent.getChildAt(i).visibility = View.GONE

        val currentSortMode = displayUtil.sortMode
        Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        popup.sortOrderContent.clearCheck()
        popup.sortOrderNamePlain.visibility = View.VISIBLE
        popup.sortOrderSongCount.visibility = View.VISIBLE
        when (currentSortMode.sortRef) {
            SortRef.GENRE_NAME -> popup.sortOrderContent.check(R.id.sort_order_name_plain)
            SortRef.SONG_COUNT -> popup.sortOrderContent.check(R.id.sort_order_song_count)
            else -> { popup.sortOrderContent.clearCheck() }
        }

        when (currentSortMode.revert) {
            false -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
            true -> popup.sortOrderBasic.check(R.id.sort_order_z_a)
        }
    }

    override fun initOnDismissListener(
        popupMenu: PopupWindow,
        popup: PopupWindowMainBinding
    ): PopupWindow.OnDismissListener {
        val displayUtil = DisplayUtil(this)
        return PopupWindow.OnDismissListener {

            //  Grid Size
            var gridSizeSelected = 0
            for (i in 0 until displayUtil.maxGridSize) {
                if ((popup.gridSize.getChildAt(i) as RadioButton).isChecked) {
                    gridSizeSelected = i + 1
                    break
                }
            }

            if (gridSizeSelected > 0 && gridSizeSelected != displayUtil.gridSize) {

                // only list no grid
                displayUtil.gridSize = gridSizeSelected
                layoutManager.spanCount = gridSizeSelected
            }

            // sort order
            val revert = when (popup.sortOrderBasic.checkedRadioButtonId) {
                R.id.sort_order_z_a -> true
                R.id.sort_order_a_z -> false
                else -> false
            }
            val sortRef = when (popup.sortOrderContent.checkedRadioButtonId) {
                R.id.sort_order_name_plain -> SortRef.GENRE_NAME
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
    }

    override fun getHeaderText(): CharSequence {
        return "${hostFragment.mainActivity.getString(R.string.genres)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "GenrePage"
    }
}
