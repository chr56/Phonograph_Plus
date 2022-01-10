/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.song.UniversalSongAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.Util

class SongPage : AbsDisplayPage<UniversalSongAdapter, GridLayoutManager>() {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var songs: List<Song> = ArrayList()

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = hostFragment.mainActivity.displayConfig.getGridSize(this) }
    }

    override fun initAdapter(): UniversalSongAdapter {
        val displayConfig = hostFragment.mainActivity.displayConfig
        val layoutRes =
            if (displayConfig.getGridSize(this) > displayConfig.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG,
            "layoutRes: ${
            if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN"
            }"
        )

        return UniversalSongAdapter(
            hostFragment.mainActivity,
            songs,
            UniversalSongAdapter.MODE_COMMON,
            layoutRes,
            null
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        coroutineScope.launch {
            songs = MediaStoreUtil.getAllSongs(App.instance) as List<Song>
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        adapter.songs = songs
    }

    override fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding) {
        val displayConfig = hostFragment.mainActivity.displayConfig

        popup.textGridSize.visibility = View.VISIBLE
        popup.gridSize.visibility = View.VISIBLE
        if (Util.isLandscape(resources))
            popup.textGridSize.text = resources.getText(R.string.action_grid_size_land)

        val current = displayConfig.getGridSize(this)
        val max = displayConfig.maxGridSize
        for (i in 0 until max) {
            popup.gridSize.getChildAt(i).visibility = View.VISIBLE
        }
        popup.gridSize.clearCheck()
        (popup.gridSize.getChildAt(current - 1) as RadioButton).isChecked = true
    }

    override fun initOnDismissListener(popupMenu: PopupWindow, popup: PopupWindowMainBinding): PopupWindow.OnDismissListener? {
        val displayConfig = hostFragment.mainActivity.displayConfig
        return PopupWindow.OnDismissListener {
            //  Grid Size
            var gridSize = 0
            for (i in 0 until displayConfig.maxGridSize) {
                if ((popup.gridSize.getChildAt(i) as RadioButton).isChecked) {
                    gridSize = i + 1
                    break
                }
            }

            if (gridSize > 0) {
                displayConfig.setGridSize(this, gridSize)
                layoutManager.spanCount = gridSize
            }
        }
    }

    companion object {
        const val TAG = "SongPage"
    }
}
