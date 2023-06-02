/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.actions.click.fileClick
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.sort.FileSortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.HomeFragment
import player.phonograph.ui.views.StatusBarView
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.core.app.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Gravity

class FilesPageExplorer(
    private val activity: ComponentActivity,
    private val homeFragment: HomeFragment,
) : AbsFilesExplorer<FilesPageViewModel>(activity) {

    private lateinit var fileModel: FilesPageViewModel

    private lateinit var adapter: FilesPageAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager


    override fun initModel(model: FilesPageViewModel) {
        fileModel = model
        // header
        binding.buttonPageHeader.setImageDrawable(activity.getThemedDrawable(R.drawable.ic_sort_variant_white_24dp))
        binding.buttonPageHeader.setOnClickListener {
            popup.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0,
                (
                        activity.findViewById<StatusBarView>(R.id.status_bar)?.height
                            ?: 8
                        ) +
                        homeFragment.totalHeaderHeight + binding.innerAppBar.height
            )
        }
        binding.buttonBack.setImageDrawable(activity.getThemedDrawable(com.afollestad.materialdialogs.R.drawable.md_nav_back))
        binding.buttonBack.setOnClickListener { gotoTopLevel(true) }
        binding.buttonBack.setOnLongClickListener {
            model.changeLocation(context, Location.HOME)
            reload()
            true
        }
        // bread crumb
        binding.header.apply {
            location = model.currentLocation.value
            callBack = {
                model.changeLocation(context, it)
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
        adapter = FilesPageAdapter(activity, model.currentFiles.value.toMutableList(), { fileEntities, position ->
            when (val item = fileEntities[position]) {
                is FileEntity.Folder -> {
                    model.changeLocation(context, item.location)
                    reload()
                }

                is FileEntity.File   -> {
                    val base = Setting.instance.songItemClickMode
                    val extra = Setting.instance.songItemClickExtraFlag
                    fileClick(
                        fileEntities,
                        position,
                        base,
                        extra,
                        activity
                    )
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
        model.refreshFiles(activity)
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
        popup.sortRefAvailable =
            arrayOf(SortRef.DISPLAY_NAME, SortRef.ADDED_DATE, SortRef.MODIFIED_DATE, SortRef.SIZE)

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

    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }
}