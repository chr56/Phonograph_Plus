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
import player.phonograph.util.ViewUtil
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
        binding.empty.visibility = View.GONE

        binding.innerAppBar.setExpanded(false)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)

        // header
        binding.buttonPageHeader.setImageDrawable(getDrawable(R.drawable.ic_refresh_white_24dp))
        binding.buttonPageHeader.setOnClickListener {
            reload()
        }
        binding.buttonBack.setImageDrawable(getDrawable(R.drawable.icon_back_white))
        binding.buttonBack.setOnClickListener { gotoTopLevel() }
        binding.buttonBack.setOnLongClickListener{
            model.currentLocation = Location.HOME
            reload()
            true
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

    private fun gotoTopLevel() {
        val parent = model.currentLocation.parent
        if (parent != null) {
            model.currentLocation = parent
            reload()
        } else {
            Snackbar.make(binding.root, getString(R.string.reached_to_root), Snackbar.LENGTH_SHORT).show()
            val ssm = hostFragment.mainActivity.getSystemService<StorageManager>()
            val volumes = StorageManagerCompat.getStorageVolumes(ssm)
            if (volumes.isEmpty()) {
                ErrorNotification.postErrorNotification("No volumes found! Your system might be not supported!")
                return
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
        }
    }

    private fun reload() {
        model.loadFiles {
            adapter.dataSet = model.currentFileList.toMutableList()
            binding.textPageHeader.text = model.currentLocation.let { "${it.storageVolume} : ${it.basePath}" }
            binding.buttonBack.setImageDrawable(
                if (model.currentLocation.parent == null)
                    getDrawable(R.drawable.ic_library_music_white_24dp)
                else
                    getDrawable(R.drawable.icon_back_white)
            )
        }
    }

    private fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return AppCompatResources.getDrawable(hostFragment.mainActivity, resId)?.also {
            it.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(
                    binding.textPageHeader.currentTextColor,
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
}
