/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.components.explorer

import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.view.View

class FilesChooserExplorerFragment : AbsFilesExplorerFragment<FilesChooserViewModel>() {

    private lateinit var fileModel: FilesChooserViewModel

    private lateinit var adapter: FilesChooserAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager


    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        fileModel = model
        binding.buttonPageHeader.visibility = View.GONE
        binding.buttonBack.setImageDrawable(requireContext().getThemedDrawable(com.afollestad.materialdialogs.R.drawable.md_nav_back))
        binding.buttonBack.setOnClickListener { gotoTopLevel(true) }
        binding.buttonBack.setOnLongClickListener {
            model.changeLocation(it.context, Location.HOME)
            true
        }
        // bread crumb
        binding.header.apply {
            location = model.currentLocation.value
            callBack = {
                model.changeLocation(context, it)
            }
        }

        binding.container.apply {
            setColorSchemeColors(ThemeColor.accentColor(activity))
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                refreshFiles()
            }
        }

        // recycle view
        layoutManager = LinearLayoutManager(activity)
        adapter = FilesChooserAdapter(activity, model.currentFiles.value.toMutableList(), {
            when (it) {
                is FileEntity.Folder -> {
                    model.changeLocation(requireContext(), it.location)
                }

                is FileEntity.File   -> {}
            }
        }, null)

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            activity,
            ThemeColor.accentColor(App.instance)
        )
        binding.recyclerView.apply {
            layoutManager = this@FilesChooserExplorerFragment.layoutManager
            adapter = this@FilesChooserExplorerFragment.adapter
        }
        model.refreshFiles(activity)

        binding.innerAppBar.setExpanded(true)
    }

}