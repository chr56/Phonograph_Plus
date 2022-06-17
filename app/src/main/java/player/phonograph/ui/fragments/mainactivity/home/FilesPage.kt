/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.storage.StorageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import lib.phonograph.storage.StorageManagerCompat
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.FileAdapter
import player.phonograph.databinding.FragmentFolderPageBinding
import player.phonograph.model.FileEntity
import player.phonograph.model.Location
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
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

        binding.innerAppBar.setExpanded(false)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)

        // header
        binding.buttonPageHeader.setImageDrawable(getDrawable(R.drawable.ic_refresh_white_24dp))
        binding.buttonPageHeader.setOnClickListener {
            reload()
        }
        binding.buttonBack.setImageDrawable(getDrawable(R.drawable.icon_back_white))
        binding.buttonBack.setOnClickListener { gotoTopLevel() }
        binding.buttonBack.setOnLongClickListener {
            model.currentLocation = Location.HOME
            reload()
            true
        }
        binding.headerTitle.text = model.currentLocation.let { "${it.storageVolume.getDescription(requireContext())} : ${it.basePath}" }
        binding.header.smoothScrollBy((binding.headerTitle.width * 0.3).toInt(), 0)

        binding.container.apply {
            setColorSchemeColors(ThemeColor.accentColor(requireContext()))
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                reload()
            }
        }

        // recycle view
        layoutManager = LinearLayoutManager(hostFragment.mainActivity)
        adapter = FileAdapter(hostFragment.mainActivity, model.currentFileList.toMutableList(), {
            when (it) {
                is FileEntity.Folder -> {
                    model.currentLocation = it.location
                    reload()
                }
                is FileEntity.File -> { MusicPlayerRemote.playNext(it.linkedSong) }
            }
        }, hostFragment)

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.apply {
            layoutManager = this@FilesPage.layoutManager
            adapter = this@FilesPage.adapter
        }
        model.loadFiles { reload() }
    }

    /**
     * @param allowToChangeVolume false if do not intend to change volume
     * @return success or not
     */
    private fun gotoTopLevel(allowToChangeVolume: Boolean = true): Boolean {
        val parent = model.currentLocation.parent
        if (parent != null) {
            model.currentLocation = parent
            reload()
            return true
        } else {
            if (!allowToChangeVolume) return false
            Snackbar.make(binding.root, getString(R.string.reached_to_root), Snackbar.LENGTH_SHORT).show()
            val ssm = hostFragment.mainActivity.getSystemService<StorageManager>()
            val volumes = StorageManagerCompat.getStorageVolumes(ssm)
            if (volumes.isEmpty()) {
                ErrorNotification.postErrorNotification("No volumes found! Your system might be not supported!")
                return false
            }
            MaterialDialog(hostFragment.mainActivity)
                .listItemsSingleChoice(
                    items = volumes.map { "${it.getDescription(requireContext())}\n(${it.directory?.path ?: "UNMOUNTED"})" },
                    waitForPositiveButton = true,
                ) { materialDialog: MaterialDialog, i: Int, _: CharSequence ->
                    materialDialog.dismiss()
                    val path = volumes[i].directory?.absolutePath
                    if (path == null) {
                        Toast.makeText(hostFragment.mainActivity, "Unmounted volume", Toast.LENGTH_SHORT).show()
                    } else { // todo
                        model.currentLocation = Location.fromAbsolutePath("$path/")
                        reload()
                    }
                }
                .title(R.string.folders)
                .positiveButton(android.R.string.ok)
                .show()
            return true
        }
    }

    private fun reload() {
        binding.container.isRefreshing = true
        model.loadFiles {
            adapter.dataSet = model.currentFileList.toMutableList()
            binding.headerTitle.text = model.currentLocation.let { "${it.storageVolume.getDescription(requireContext())} : ${it.basePath}" }
            binding.header.smoothScrollBy((binding.headerTitle.width * 0.6).toInt(), 0)
            binding.buttonBack.setImageDrawable(
                if (model.currentLocation.parent == null)
                    getDrawable(R.drawable.ic_library_music_white_24dp)
                else
                    getDrawable(R.drawable.icon_back_white)
            )
            binding.container.isRefreshing = false
        }
    }

    private fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return AppCompatResources.getDrawable(hostFragment.mainActivity, resId)?.also {
            it.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(
                    binding.headerTitle.currentTextColor,
                    BlendModeCompat.SRC_IN
                )
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

    override fun onBackPress(): Boolean {
        return gotoTopLevel(false)
    }
}
