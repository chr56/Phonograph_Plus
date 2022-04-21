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
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.helper.SortOrder
import player.phonograph.mediastore.GenreLoader
import player.phonograph.model.Genre
import player.phonograph.util.Util

class GenrePage : AbsDisplayPage<Genre, DisplayAdapter<Genre>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Genre> {
        return DisplayAdapter(
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
//        popup.sortOrderContent.visibility = View.VISIBLE
//        popup.textSortOrderContent.visibility = View.VISIBLE
//        for (i in 0 until popup.sortOrderContent.childCount) popup.sortOrderContent.getChildAt(i).visibility = View.GONE

        val currentSortOrder = displayUtil.sortOrder
        Log.d(TAG, "Read cfg: $currentSortOrder")

        // todo
        when (currentSortOrder) {
            SortOrder.GenreSortOrder.GENRE_Z_A
            -> popup.sortOrderBasic.check(R.id.sort_order_z_a)
            SortOrder.GenreSortOrder.GENRE_A_Z
            -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
            else -> popup.sortOrderBasic.clearCheck()
        }

//        popup.sortOrder__.visibility = View.VISIBLE
//        when (currentSortOrder) {
//            SortOrder.GenreSortOrder.GENRE_A_Z,SortOrder.GenreSortOrder.GENRE_Z_A -> popup.sortOrderContent.check(R.id.sort_order_)
//            else -> { popup.sortOrderContent.clearCheck() }
//        }
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
            val sortOrderSelected: String =
                when (popup.sortOrderBasic.checkedRadioButtonId) {
                    R.id.sort_order_a_z -> SortOrder.GenreSortOrder.GENRE_A_Z
                    R.id.sort_order_z_a -> SortOrder.GenreSortOrder.GENRE_Z_A
                    else -> ""
                }

            if (sortOrderSelected.isNotBlank() && displayUtil.sortOrder != sortOrderSelected) {
                displayUtil.sortOrder = sortOrderSelected
                loadDataSet()
                Log.d(TAG, "Write cfg: $sortOrderSelected")
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
