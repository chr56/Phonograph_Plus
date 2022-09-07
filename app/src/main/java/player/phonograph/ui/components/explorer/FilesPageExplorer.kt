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
import player.phonograph.adapter.file.FilePageAdapter
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
) : AbsFilesExplorer<FilesPageViewModel>(activity) {

    private lateinit var fileModel: FilesPageViewModel

    private lateinit var adapter: FilePageAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager


    override fun initModel(model: FilesPageViewModel) {
        fileModel = model
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
            model.currentLocation = Location.HOME
            reload()
            true
        }
        // bread crumb
        binding.header.apply {
            location = model.currentLocation
            callBack = {
                model.currentLocation = it
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
        adapter = FilePageAdapter(activity, model.currentFileList.toMutableList(), {
            when (it) {
                is FileEntity.Folder -> {
                    model.currentLocation = it.location
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
        model.loadFiles { reload() }
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
        popup.useLegacyListFiles = fileModel.useLegacyListFile
        popup.showFilesImages = fileModel.showFilesImages
    }

    private fun dismissPopup(popup: ListOptionsPopup) {
        Setting.instance.fileSortMode = FileSortMode(popup.sortRef, popup.revert)
        fileModel.useLegacyListFile = popup.useLegacyListFiles
        if (fileModel.showFilesImages != popup.showFilesImages) {
            fileModel.showFilesImages = popup.showFilesImages
            adapter.loadCover = fileModel.showFilesImages
            adapter.notifyDataSetChanged()
        }
        reload()
    }

    /**
     * @param allowToChangeVolume false if do not intend to change volume
     * @return success or not
     */
    override fun gotoTopLevel(allowToChangeVolume: Boolean): Boolean {
        val parent = fileModel.currentLocation.parent
        return if (parent != null) {
            fileModel.currentLocation = parent
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
                initialSelection = volumes.indexOf(fileModel.currentLocation.storageVolume),
                waitForPositiveButton = true,
            ) { materialDialog: MaterialDialog, i: Int, _: CharSequence ->
                materialDialog.dismiss()
                val path = volumes[i].root()?.absolutePath
                if (path == null) {
                    Toast.makeText(activity, "Unmounted volume", Toast.LENGTH_SHORT).show()
                } else { // todo
                    fileModel.currentLocation = Location.fromAbsolutePath("$path/")
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
        fileModel.loadFiles {
            adapter.dataSet = fileModel.currentFileList.toMutableList()
            binding.header.apply {
                location = fileModel.currentLocation
                layoutManager.scrollHorizontallyBy(
                    binding.header.width / 4,
                    recyclerView.Recycler(),
                    RecyclerView.State()
                )
            }
            binding.buttonBack.setImageDrawable(
                if (fileModel.currentLocation.parent == null) {
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