/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.graphics.drawable.Drawable
import android.os.Environment
import android.os.storage.StorageManager
import android.view.View.GONE
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
import player.phonograph.adapter.file.FileChooserAdapter
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.notification.ErrorNotification
import player.phonograph.ui.fragments.HomeFragment
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor

class FilesChooserExplorer(
    private val activity: ComponentActivity,
) : AbsFilesExplorer<FilesChooserViewModel>(activity) {
    private lateinit var fileModel: FilesChooserViewModel

    private lateinit var adapter: FileChooserAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun initModel(model: FilesChooserViewModel) {
        fileModel = model
        binding.buttonPageHeader.visibility = GONE
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
        adapter = FileChooserAdapter(activity, model.currentFileList.toMutableList(), {
            when (it) {
                is FileEntity.Folder -> {
                    model.currentLocation = it.location
                    reload()
                }
                is FileEntity.File -> {}
            }
        }, null)

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            activity,
            ThemeColor.accentColor(App.instance)
        )
        binding.recyclerView.apply {
            layoutManager = this@FilesChooserExplorer.layoutManager
            adapter = this@FilesChooserExplorer.adapter
        }
        model.loadFiles(activity) { reload() }

        binding.innerAppBar.setExpanded(true)
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
        fileModel.loadFiles(activity) {
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