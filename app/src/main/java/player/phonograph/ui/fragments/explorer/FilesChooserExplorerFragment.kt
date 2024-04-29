/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.explorer

import mt.pref.ThemeColor
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.R as MDR

class FilesChooserExplorerFragment : AbsFilesExplorerFragment<FilesChooserViewModel, FilesChooserAdapter>() {

    override val model: FilesChooserViewModel by viewModels({ requireActivity() })

    override lateinit var adapter: FilesChooserAdapter
    override lateinit var layoutManager: LinearLayoutManager


    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val accentColor = ThemeColor.accentColor(requireContext())
        super.onViewCreated(view, savedInstanceState)

        binding.buttonPageHeader.visibility = View.GONE
        binding.buttonBack.setImageDrawable(requireContext().getThemedDrawable(MDR.drawable.md_nav_back))
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
            setColorSchemeColors(accentColor)
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                refreshFiles()
            }
        }

        // recycle view
        layoutManager = LinearLayoutManager(requireContext())
        adapter = FilesChooserAdapter(requireActivity(), model.currentFiles.value) {
            when (it) {
                is FileEntity.Folder -> model.changeLocation(requireContext(), it.location)
                is FileEntity.File   -> {}
            }
        }

        binding.recyclerView.setUpFastScrollRecyclerViewColor(requireActivity(), accentColor)
        binding.recyclerView.apply {
            layoutManager = this@FilesChooserExplorerFragment.layoutManager
            adapter = this@FilesChooserExplorerFragment.adapter
        }
        model.refreshFiles(requireContext())

        binding.innerAppBar.setExpanded(true)
    }

}