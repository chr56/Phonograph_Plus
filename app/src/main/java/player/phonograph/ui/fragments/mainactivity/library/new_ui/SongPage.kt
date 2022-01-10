/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import player.phonograph.adapter.song.UniversalSongAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.model.Song

class SongPage : AbsDisplayPage<UniversalSongAdapter, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
    }

    override fun initAdapter(): UniversalSongAdapter {
        val dataSet = MutableList(15) {
            Song(0, "test", 0, 0, 0, "/storage/emulated/0/Music/todo.mp3", 0, 0, "todo", 0, "NA")
        }
        return UniversalSongAdapter(
            hostFragment.mainActivity,
            dataSet,
            UniversalSongAdapter.MODE_COMMON,
            null
        )
    }

    override fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding) {
//        TODO("Not yet implemented")
    }
}
