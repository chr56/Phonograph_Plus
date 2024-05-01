/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.fragments.explorer

import player.phonograph.R
import player.phonograph.actions.ClickActionProviders
import player.phonograph.model.file.FileEntity
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.MainFragment
import player.phonograph.ui.views.StatusBarView
import androidx.fragment.app.viewModels
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import java.lang.ref.SoftReference

class FilesPageExplorerFragment : AbsFilesExplorerFragment<FilesPageViewModel, FilesPageAdapter>() {

    override val model: FilesPageViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }

    override fun onPrepareHeader() {
        binding.buttonOptions.visibility = View.GONE
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