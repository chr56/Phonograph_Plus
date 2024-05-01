/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.fragments.explorer

import player.phonograph.model.file.FileEntity
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.View

class FilesChooserExplorerFragment : AbsFilesExplorerFragment<FilesChooserViewModel, FilesChooserAdapter>() {

    override val model: FilesChooserViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }

    override fun onPrepareHeader() {
        binding.buttonOptions.visibility = View.GONE
    }

    override fun createAdapter(): FilesChooserAdapter =
        FilesChooserAdapter(requireActivity(), model.currentFiles.value) {
            when (it) {
                is FileEntity.Folder -> onSwitch(it.location)
                is FileEntity.File   -> {}
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.navigationHeader.setExpanded(true)
    }
}