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
import player.phonograph.adapter.display.ArtistDisplayAdapter
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Artist
import player.phonograph.util.Util

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
            val temp = ArtistLoader.getAllArtists(App.instance) as List<Artist>
            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataset = temp
            }
        }
    }

    override fun getDataSet(): List<Artist> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding) {
        val displayUtil = DisplayUtil(this)

        // grid size
        popup.textGridSize.visibility = View.VISIBLE
        popup.gridSize.visibility = View.VISIBLE
        if (Util.isLandscape(resources)) popup.textGridSize.text = resources.getText(R.string.action_grid_size_land)
        val current = displayUtil.gridSize
        val max = displayUtil.maxGridSize
        for (i in 0 until max) popup.gridSize.getChildAt(i).visibility = View.VISIBLE
        popup.gridSize.clearCheck()
        (popup.gridSize.getChildAt(current - 1) as RadioButton).isChecked = true

        // color footer
        popup.actionColoredFooters.visibility = View.VISIBLE
        popup.actionColoredFooters.isChecked = displayUtil.colorFooter
        popup.actionColoredFooters.isEnabled = displayUtil.gridSize > displayUtil.maxGridSizeForList

        // sort order
        popup.sortOrderBasic.visibility = View.VISIBLE
        popup.textSortOrderBasic.visibility = View.VISIBLE
        popup.sortOrderContent.visibility = View.VISIBLE
        popup.textSortOrderContent.visibility = View.VISIBLE
        for (i in 0 until popup.sortOrderContent.childCount) popup.sortOrderContent.getChildAt(i).visibility = View.GONE

        val currentSortMode = displayUtil.sortMode
        Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        popup.sortOrderContent.clearCheck()
        popup.sortOrderArtist.visibility = View.VISIBLE
        popup.sortOrderAlbumCount.visibility = View.VISIBLE
        popup.sortOrderSongCount.visibility = View.VISIBLE
        when (currentSortMode.sortRef) {
            SortRef.ARTIST_NAME -> popup.sortOrderContent.check(R.id.sort_order_artist)
            SortRef.ALBUM_COUNT -> popup.sortOrderContent.check(R.id.sort_order_album_count)
            SortRef.SONG_COUNT -> popup.sortOrderContent.check(R.id.sort_order_song_count)
            else -> { popup.sortOrderContent.clearCheck() }
        }

        when (currentSortMode.revert) {
            false -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
            true -> popup.sortOrderBasic.check(R.id.sort_order_z_a)
        }
    }

    override fun initOnDismissListener(popupMenu: PopupWindow, popup: PopupWindowMainBinding): PopupWindow.OnDismissListener {
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

                displayUtil.gridSize = gridSizeSelected
                val itemLayoutRes =
                    if (gridSizeSelected > displayUtil.maxGridSizeForList) R.layout.item_grid else R.layout.item_list

                if (adapter.layoutRes != itemLayoutRes) {
                    loadDataSet()
                    initRecyclerView() // again
                }
                layoutManager.spanCount = gridSizeSelected
            }

            // color footer
            val coloredFootersSelected = popup.actionColoredFooters.isChecked
            if (displayUtil.colorFooter != coloredFootersSelected) {
                displayUtil.colorFooter = coloredFootersSelected
                adapter.usePalette = coloredFootersSelected
                adapter.dataset = getDataSet() // just refresh
            }

            // sort order
            val revert = when (popup.sortOrderBasic.checkedRadioButtonId) {
                R.id.sort_order_z_a -> true
                R.id.sort_order_a_z -> false
                else -> false
            }
            val sortRef = when (popup.sortOrderContent.checkedRadioButtonId) {
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
    }

    override fun getHeaderText(): CharSequence {
        return "${hostFragment.mainActivity.getString(R.string.artists)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "ArtistPage"
    }
}
