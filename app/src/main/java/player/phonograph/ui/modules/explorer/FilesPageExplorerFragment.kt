/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.mechanism.actions.ClickActionProviders
import player.phonograph.model.file.FileEntity
import androidx.fragment.app.viewModels

class FilesPageExplorerFragment : AbsFilesExplorerFragment<FilesPageViewModel, FilesPageAdapter>() {

    override val model: FilesPageViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed(items: List<FileEntity>) {
        adapter.dataSet = items.toMutableList()
    }

    override fun createAdapter(): FilesPageAdapter =
        FilesPageAdapter(requireActivity(), model.currentFiles.value) { fileEntities, position ->
            when (val item = fileEntities[position]) {
                is FileEntity.Folder -> {
                    onSwitch(item.location)
                }

                is FileEntity.File   -> {
                    ClickActionProviders.FileEntityClickActionProvider().listClick(
                        fileEntities,
                        position,
                        requireContext(),
                        null
                    )
                }
            }
        }//todo

}