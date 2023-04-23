/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.view.View.GONE
import androidx.core.app.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor

class FilesChooserExplorer(
    private val activity: ComponentActivity,
) : AbsFilesExplorer<FilesChooserViewModel>(activity) {
    private lateinit var fileModel: FilesChooserViewModel

    private lateinit var adapter: FilesChooserAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun initModel(model: FilesChooserViewModel) {
        fileModel = model
        binding.buttonPageHeader.visibility = GONE
        binding.buttonBack.setImageDrawable(activity.getThemedDrawable(com.afollestad.materialdialogs.R.drawable.md_nav_back))
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
        adapter = FilesChooserAdapter(activity, model.currentFileList.toMutableList(), {
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

    override fun updateFilesDisplayed() {
        adapter.dataSet = fileModel.currentFileList.toMutableList()
    }
}