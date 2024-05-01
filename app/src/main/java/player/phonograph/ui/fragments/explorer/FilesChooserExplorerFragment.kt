/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.fragments.explorer

import player.phonograph.model.file.FileEntity
import androidx.fragment.app.viewModels

class FilesChooserExplorerFragment : AbsFilesExplorerFragment<FilesChooserViewModel, FilesChooserAdapter>() {

    override val model: FilesChooserViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }

    override fun createAdapter(): FilesChooserAdapter =
        FilesChooserAdapter(requireActivity(), model.currentFiles.value) {
            when (it) {
                is FileEntity.Folder -> onSwitch(it.location)
                is FileEntity.File   -> {}
            }
        }

}