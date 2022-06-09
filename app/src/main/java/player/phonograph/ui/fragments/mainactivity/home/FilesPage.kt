/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.FileAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.model.FileEntity
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.ViewUtil
import util.mdcolor.pref.ThemeColor

class FilesPage : AbsPage() {

    private val model: FilesViewModel by viewModels()

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        model.loadFiles { }
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
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

        val actionDrawable = AppCompatResources.getDrawable(
            hostFragment.mainActivity,
            R.drawable.ic_refresh_white_24dp
        )
        actionDrawable?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(
                binding.textPageHeader.currentTextColor,
                BlendModeCompat.SRC_IN
            )
        binding.buttonPageHeader.setImageDrawable(actionDrawable)
        binding.buttonPageHeader.setOnClickListener {
            model.loadFiles {
                adapter.dataSet = model.currentFileList
            }
        }
        binding.textPageHeader.text = model.currentLocation.let { "${it.storageVolume} : ${it.basePath}" }

        layoutManager = LinearLayoutManager(hostFragment.mainActivity)
        adapter = FileAdapter(hostFragment.mainActivity, model.currentFileList, {
            if (it.isFolder) {
                model.currentLocation = it.path
                model.loadFiles {
                    adapter.dataSet = model.currentFileList
                }
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
