/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.adapter.PlaylistAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.model.Playlist
import player.phonograph.model.smartplaylist.HistoryPlaylist
import player.phonograph.model.smartplaylist.LastAddedPlaylist
import player.phonograph.model.smartplaylist.MyTopTracksPlaylist
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.ViewUtil
import java.util.ArrayList

class PlaylistPage : AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadPlaylist()
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var adapter: PlaylistAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver

    private var isRecyclerViewPrepared: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.innerAppBar.visibility = View.GONE

        layoutManager = LinearLayoutManager(requireActivity())

        adapter = PlaylistAdapter(
            hostFragment.mainActivity,
            ArrayList<Playlist>(), R.layout.item_list_single_row,
            null // todo
        )

        ViewUtil.setUpFastScrollRecyclerViewColor(
            requireActivity(),
            binding.recyclerView,
            ThemeColor.accentColor(requireActivity())
        )
        binding.recyclerView.apply {
            layoutManager = layoutManager
            adapter = adapter
        }
        isRecyclerViewPrepared = true

        adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
            }
        }
        adapter.registerAdapterDataObserver(adapterDataObserver)
    }

    private val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private fun loadPlaylist() {
        loaderCoroutineScope.launch {
            val context = hostFragment.mainActivity
            val temp = mutableListOf<Playlist>(
                LastAddedPlaylist(context),
                HistoryPlaylist(context),
                MyTopTracksPlaylist(context)
            ).also { it.addAll(MediaStoreUtil.getAllPlaylists(context)) }

            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataSet = temp
            }
        }
    }

    private val emptyMessage: Int = R.string.no_playlists
    private fun checkEmpty() {
        if (isRecyclerViewPrepared) {
            binding.empty.setText(emptyMessage)
            binding.empty.visibility = if (adapter.dataSet.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
        isRecyclerViewPrepared = false
    }
}
