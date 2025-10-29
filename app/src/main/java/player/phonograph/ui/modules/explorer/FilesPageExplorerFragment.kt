/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.util.observe
import player.phonograph.util.ui.BottomViewWindowInsetsController
import player.phonograph.util.ui.applyControllableWindowInsetsAsBottomView
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.View

class FilesPageExplorerFragment : AbsFilesExplorerFragment<FilesPageViewModel, FilesPageAdapter>() {

    override val model: FilesPageViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed(items: List<FileItem>) {
        adapter.dataSet = items.toMutableList()
    }

    override fun createAdapter(): FilesPageAdapter =
        FilesPageAdapter(requireActivity(), model.currentFiles.value) { fileEntities, position ->
            val item = fileEntities[position]
            if (item.isFolder) {
                onSwitch(item.mediaPath)
            } else {
                ClickActionProviders.FileEntityClickActionProvider().listClick(
                    fileEntities,
                    position,
                    requireContext(),
                    null
                )
            }
        }//todo


    //region WindowInsets
    private val panelViewModel: PanelViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomViewWindowInsetsController = binding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }
    //endregion

}