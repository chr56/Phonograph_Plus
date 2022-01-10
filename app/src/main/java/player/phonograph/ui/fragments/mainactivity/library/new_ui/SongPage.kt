/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.adapter.song.UniversalSongAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil

class SongPage : AbsDisplayPage<UniversalSongAdapter, GridLayoutManager>() {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var songs: List<Song> = ArrayList()

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
    }

    override fun initAdapter(): UniversalSongAdapter {
//        val dataSet = MutableList(15) {
//            Song(0, "test", 0, 0, 0, "/storage/emulated/0/Music/demo.mp3", 0, 0, "demo", 0, "NA")
//        }
        return UniversalSongAdapter(
            hostFragment.mainActivity,
            songs,
            UniversalSongAdapter.MODE_COMMON,
            null
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        coroutineScope.launch {
            songs = MediaStoreUtil.getAllSongs(App.instance) as List<Song>
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

//    override fun onStart() {
//        super.onStart()
//        adapter.songs = songs
//    }

    override fun onResume() {
        super.onResume()
        adapter.songs = songs
    }

    override fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding) {
//        TODO("Not yet implemented")
    }
}
