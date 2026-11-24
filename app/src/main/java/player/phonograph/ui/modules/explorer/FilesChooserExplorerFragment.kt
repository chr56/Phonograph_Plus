/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.actions.ItemClickActions
import android.content.Context
import android.os.Bundle
import android.view.View

class FilesChooserExplorerFragment : AbsFilesExplorerFragment() {

    override val allowMultiSelection: Boolean = false

    override fun clickAction(context: Context, item: FileItem, position: Int, list: List<FileItem>): ActionEntry? =
        if (item.isFolder) ItemClickActions.Folder(item, ::onSwitch) else null

    override val showMenu: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.optionUseLegacyListFile = true
    }
}