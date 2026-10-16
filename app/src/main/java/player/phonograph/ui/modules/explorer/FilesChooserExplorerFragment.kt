/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import player.phonograph.ui.actions.ClickActionProviders
import android.content.Context
import android.os.Bundle
import android.view.View

class FilesChooserExplorerFragment : AbsFilesExplorerFragment() {

    override val allowMultiSelection: Boolean = false

    override fun createClickActionProvider(): ClickActionProviders.ClickActionProvider<FileItem> =
        FilesChooserClickActionProvider(::onSwitch)

    override val showMenu: Boolean = false

    class FilesChooserClickActionProvider(private val onSwitch: (FileItem) -> Unit) :
            ClickActionProviders.ClickActionProvider<FileItem> {
        override fun listClick(
            context: Context,
            list: List<FileItem>,
            position: Int,
        ): Boolean {
            val item = list[position]
            when {
                item.isFolder -> onSwitch(item)
                else          -> Unit
            }
            return true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.optionUseLegacyListFile = true
    }
}