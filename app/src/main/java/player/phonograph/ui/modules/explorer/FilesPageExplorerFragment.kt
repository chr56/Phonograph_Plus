/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingObserver
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.util.observe
import player.phonograph.util.ui.BottomViewWindowInsetsController
import player.phonograph.util.ui.applyControllableWindowInsetsAsBottomView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView

class FilesPageExplorerFragment : AbsFilesExplorerFragment() {

    override val allowMultiSelection: Boolean = true

    override fun createClickActionProvider(): ClickActionProviders.ClickActionProvider<FileItem> =
        FilesPageClickActionProvider(::onSwitch)

    override fun createMenuProvider(): ActionMenuProviders.ActionMenuProvider<FileItem>? =
        ActionMenuProviders.FileItemActionMenuProvider

    class FilesPageClickActionProvider(private val onSwitch: (FileItem) -> Unit) :
            ClickActionProviders.ClickActionProvider<FileItem> {

        private val provider = ClickActionProviders.FileEntityClickActionProvider()

        override fun listClick(
            list: List<FileItem>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            val item = list[position]
            when {
                item.isFolder -> onSwitch(item)
                else          -> provider.listClick(list, position, context, imageView)
            }
            return true
        }
    }

    //region WindowInsets
    private val panelViewModel: PanelViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController
    //endregion

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomViewWindowInsetsController = binding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }

        SettingObserver(view.context, lifecycleScope).apply {
            collect(Keys.showFileImages) { value ->
                (adapter.presenter as? FileItemPresenter)?.loadCover = value
                @SuppressLint("NotifyDataSetChanged")
                adapter.notifyDataSetChanged()
            }
            collect(Keys.useLegacyListFilesImpl) { value ->
                model.optionUseLegacyListFile = value
            }
        }
    }

}