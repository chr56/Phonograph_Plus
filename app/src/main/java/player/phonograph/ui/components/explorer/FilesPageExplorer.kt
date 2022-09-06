/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.graphics.drawable.Drawable
import android.os.Environment
import android.os.storage.StorageManager
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.app.ComponentActivity
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.snackbar.Snackbar
import lib.phonograph.storage.root
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.FileAdapter
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.sort.FileSortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.HomeFragment
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor

class FilesPageExplorer(
    private val activity: ComponentActivity,
    private val homeFragment: HomeFragment,
) : FilesExplorer<FilesViewModel>(activity) {

    private lateinit var model: FilesViewModel

    private lateinit var adapter: FileAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager


    override fun initModel(filesViewModel: FilesViewModel) {
        model = filesViewModel
        // header
        binding.buttonPageHeader.setImageDrawable(getDrawable(R.drawable.ic_sort_variant_white_24dp))
        binding.buttonPageHeader.setOnClickListener {
            popup.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0,
                (
                        activity.findViewById<player.phonograph.views.StatusBarView>(R.id.status_bar)?.height ?: 8
                        ) +
                        homeFragment.totalHeaderHeight + binding.innerAppBar.height
            )
        }
        binding.buttonBack.setImageDrawable(getDrawable(R.drawable.md_nav_back))
        binding.buttonBack.setOnClickListener { gotoTopLevel(true) }
        binding.buttonBack.setOnLongClickListener {
            filesViewModel.currentLocation = Location.HOME
            reload()
            true
        }
        // bread crumb
        binding.header.apply {
            location = filesViewModel.currentLocation
            callBack = {
                filesViewModel.currentLocation = it
                reload()
            }
        }

        binding.container.apply {
            setColorSchemeColors(ThemeColor.accentColor(activity))
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                reload()
            }
        }

        // recycle view
        layoutManager = LinearLayoutManager(activity)
        adapter = FileAdapter(activity, filesViewModel.currentFileList.toMutableList(), {
            when (it) {
                is FileEntity.Folder -> {
                    filesViewModel.currentLocation = it.location
                    reload()
                }
                is FileEntity.File -> {
                    MusicPlayerRemote.playNow(it.linkedSong)
                }
            }
        }, homeFragment.cabController)

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            activity,
            ThemeColor.accentColor(App.instance)
        )
        binding.recyclerView.apply {
            layoutManager = this@FilesPageExplorer.layoutManager
            adapter = this@FilesPageExplorer.adapter
        }
        filesViewModel.loadFiles { reload() }
    }

    private val popup: ListOptionsPopup by lazy(LazyThreadSafetyMode.NONE) {
        ListOptionsPopup(context).also {
            it.onShow = this::configPopup
            it.onDismiss = this::dismissPopup
        }
    }

    private fun configPopup(popup: ListOptionsPopup) {
        val currentSortMode = Setting.instance.fileSortMode
        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable = arrayOf(SortRef.DISPLAY_NAME, SortRef.ADDED_DATE, SortRef.MODIFIED_DATE, SortRef.SIZE)

        popup.showFileOption = true
        popup.useLegacyListFiles = model.useLegacyListFile
        popup.showFilesImages = model.showFilesImages
    }

    private fun dismissPopup(popup: ListOptionsPopup) {
        Setting.instance.fileSortMode = FileSortMode(popup.sortRef, popup.revert)
        model.useLegacyListFile = popup.useLegacyListFiles
        if (model.showFilesImages != popup.showFilesImages) {
            model.showFilesImages = popup.showFilesImages
            adapter.loadCover = model.showFilesImages
            adapter.notifyDataSetChanged()
        }
        reload()
    }

    /**
     * @param allowToChangeVolume false if do not intend to change volume
     * @return success or not
     */
    override fun gotoTopLevel(allowToChangeVolume: Boolean): Boolean {
        val parent = model.currentLocation.parent
        return if (parent != null) {
            model.currentLocation = parent
            reload()
            true
        } else {
            Snackbar.make(binding.root, context.getString(R.string.reached_to_root), Snackbar.LENGTH_SHORT).show()
            if (!allowToChangeVolume) {
                false
            } else {
                changeVolume()
            }
        }
    }

    override fun changeVolume(): Boolean {
        val storageManager = activity.getSystemService<StorageManager>()
        val volumes = storageManager?.storageVolumes
            ?.filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
            ?: emptyList()
        if (volumes.isEmpty()) {
            ErrorNotification.postErrorNotification("No volumes found! Your system might be not supported!")
            return false
        }
        MaterialDialog(activity)
            .listItemsSingleChoice(
                items = volumes.map { "${it.getDescription(context)}\n(${it.root()?.path ?: "N/A"})" },
                initialSelection = volumes.indexOf(model.currentLocation.storageVolume),
                waitForPositiveButton = true,
            ) { materialDialog: MaterialDialog, i: Int, _: CharSequence ->
                materialDialog.dismiss()
                val path = volumes[i].root()?.absolutePath
                if (path == null) {
                    Toast.makeText(activity, "Unmounted volume", Toast.LENGTH_SHORT).show()
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

    override fun reload() {
        binding.container.isRefreshing = true
        model.loadFiles {
            adapter.dataSet = model.currentFileList.toMutableList()
            binding.header.apply {
                location = model.currentLocation
                layoutManager.scrollHorizontallyBy(
                    binding.header.width / 4,
                    recyclerView.Recycler(),
                    RecyclerView.State()
                )
            }
            binding.buttonBack.setImageDrawable(
                if (model.currentLocation.parent == null) {
                    getDrawable(R.drawable.ic_library_music_white_24dp)
                } else {
                    getDrawable(R.drawable.icon_back_white)
                }
            )
            binding.container.isRefreshing = false
        }
    }

    private fun getDrawable(@DrawableRes resId: Int): Drawable? =
        activity.getTintedDrawable(resId, activity.primaryTextColor(activity.resources.nightMode))
}