/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.App
import player.phonograph.adapter.FileAdapter
import player.phonograph.databinding.FragmentFolderBinding
import player.phonograph.model.FileEntity
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.ViewUtil
import util.mdcolor.pref.ThemeColor

class FilesPage : AbsPage() {

    private val model: FilesViewModel by viewModels()

    private var _viewBinding: FragmentFolderBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        model.loadFiles()
        _viewBinding = FragmentFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }

    lateinit var adapter: FileAdapter
    lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.empty.visibility = View.GONE

        layoutManager = LinearLayoutManager(hostFragment.mainActivity)
        adapter = FileAdapter(hostFragment.mainActivity, model.currentFileList, {
            if (it.isFolder) {
                model.currentLocation = it.path
                model.loadFiles()
            } else {
                MusicPlayerRemote.playNext((it as FileEntity.File).linkedSong)
            }
        }, null)

        ViewUtil.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity, binding.recyclerView,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.apply {
            layoutManager = this@FilesPage.layoutManager
            adapter = this@FilesPage.adapter
        }
    }
}
