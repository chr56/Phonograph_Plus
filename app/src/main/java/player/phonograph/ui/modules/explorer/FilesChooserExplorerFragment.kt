/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import android.content.Context
import android.widget.ImageView

class FilesChooserExplorerFragment : AbsFilesExplorerFragment() {

    override val allowMultiSelection: Boolean = false

    override fun createClickActionProvider(): ClickActionProviders.ClickActionProvider<FileItem> =
        FilesChooserClickActionProvider(::onSwitch)

    override fun createMenuProvider(): ActionMenuProviders.ActionMenuProvider<FileItem>? = null

    class FilesChooserClickActionProvider(private val onSwitch: (FileItem) -> Unit) :
            ClickActionProviders.ClickActionProvider<FileItem> {
        override fun listClick(
            list: List<FileItem>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            val item = list[position]
            when {
                item.isFolder -> onSwitch(item)
                else          -> Unit
            }
            return true
        }
    }
}