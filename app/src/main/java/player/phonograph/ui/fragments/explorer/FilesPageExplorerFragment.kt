/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.fragments.explorer

import player.phonograph.actions.ClickActionProviders
import player.phonograph.model.file.FileEntity
import androidx.fragment.app.viewModels

class FilesPageExplorerFragment : AbsFilesExplorerFragment<FilesPageViewModel, FilesPageAdapter>() {

    override val model: FilesPageViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
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