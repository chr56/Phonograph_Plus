/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import androidx.fragment.app.viewModels

class FilesChooserExplorerFragment : AbsFilesExplorerFragment<FilesChooserViewModel, FilesChooserAdapter>() {

    override val model: FilesChooserViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed(items: List<FileItem>) {
        adapter.dataSet = items.toMutableList()
    }

    override fun createAdapter(): FilesChooserAdapter =
        FilesChooserAdapter(requireActivity(), model.currentFiles.value) {
            when {
                it.isFolder -> onSwitch(it.mediaPath)
                else        -> Unit
            }
        }

}