/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingsObserver
import player.phonograph.ui.actions.ItemClickActions
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.ui.util.BottomViewWindowInsetsController
import player.phonograph.ui.util.applyControllableWindowInsetsAsBottomView
import player.phonograph.ui.util.observe
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.Job

class FilesPageExplorerFragment : AbsFilesExplorerFragment() {

    override val allowMultiSelection: Boolean = true

    override fun clickAction(context: Context, item: FileItem, position: Int, list: List<FileItem>): ActionEntry? =
        if (item.isFolder) {
            ItemClickActions.Folder(item, ::onSwitch)
        } else {
            ItemClickActions.File(context, item, position, list)
        }

    override val showMenu: Boolean = false

    //region WindowInsets
    private val panelViewModel: PanelViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController
    //endregion

    private var jobs = mutableListOf<Job>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomViewWindowInsetsController = binding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isMiniPlayerHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }

        SettingsObserver(view.context, lifecycleScope).apply {
            jobs += collect(Keys.showFileImages) { value ->
                adapter.options = adapter.options.copy(disableImage = !value)
                @SuppressLint("NotifyDataSetChanged")
                adapter.notifyDataSetChanged()
            }
            jobs += collect(Keys.useLegacyListFilesImpl) { value ->
                model.optionUseLegacyListFile = value
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (it in jobs) {
            it.cancel()
        }
    }
}