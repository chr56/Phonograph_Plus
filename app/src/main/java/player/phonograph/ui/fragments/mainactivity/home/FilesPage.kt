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
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.FileAdapter
import player.phonograph.databinding.FragmentFolderPageBinding
import player.phonograph.model.FileEntity
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.ViewUtil
import util.mdcolor.pref.ThemeColor

class FilesPage : AbsPage() {

    private val model: FilesViewModel by viewModels()

    private var _viewBinding: FragmentFolderPageBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _viewBinding = FragmentFolderPageBinding.inflate(inflater, container, false)
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

        binding.innerAppBar.setExpanded(false)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)

        // header
        val refreshDrawable = AppCompatResources.getDrawable(
            hostFragment.mainActivity,
            R.drawable.ic_refresh_white_24dp
        )
        refreshDrawable?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(
                binding.textPageHeader.currentTextColor,
                BlendModeCompat.SRC_IN
            )
        val backDrawable = AppCompatResources.getDrawable(
            hostFragment.mainActivity,
            R.drawable.icon_back_white
        )
        backDrawable?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(
                binding.textPageHeader.currentTextColor,
                BlendModeCompat.SRC_IN
            )
        binding.buttonPageHeader.setImageDrawable(refreshDrawable)
        binding.buttonPageHeader.setOnClickListener {
            reload()
        }
        binding.buttonBack.setImageDrawable(backDrawable)
        binding.buttonBack.setOnClickListener {
            val parent = model.currentLocation.parent
            if (parent != null) {
                model.currentLocation = parent
                reload()
            } else {
                Snackbar.make(binding.root, getString(R.string.reached_to_root), Snackbar.LENGTH_SHORT).show()
                // todo volume selector
            }
        }
        binding.textPageHeader.text = model.currentLocation.let { "${it.storageVolume} : ${it.basePath}" }

        // recycle view
        layoutManager = LinearLayoutManager(hostFragment.mainActivity)
        adapter = FileAdapter(hostFragment.mainActivity, model.currentFileList.toMutableList(), {
            if (it.isFolder) {
                model.currentLocation = it.path
                reload()
            } else {
                MusicPlayerRemote.playNext((it as FileEntity.File).linkedSong)
            }
        }, hostFragment)

        ViewUtil.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity, binding.recyclerView,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.apply {
            layoutManager = this@FilesPage.layoutManager
            adapter = this@FilesPage.adapter
        }
        model.loadFiles { reload() }
    }

    private fun reload() {
        model.loadFiles {
            adapter.dataSet = model.currentFileList.toMutableList()
            binding.textPageHeader.text = model.currentLocation.let { "${it.storageVolume} : ${it.basePath}" }
        }
    }

    private var innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.innerAppBar.totalScrollRange + verticalOffset,
                binding.container.paddingRight,
                binding.container.paddingBottom

            )
        }
}
