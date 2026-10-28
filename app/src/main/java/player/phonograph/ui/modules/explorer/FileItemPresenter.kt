/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.explorer

import coil.request.Disposable
import coil.target.Target
import player.phonograph.R
import player.phonograph.foundation.dateTimeText
import player.phonograph.mechanism.coil.loadImage
import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.actions.ActionMenus
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.themeIconColor
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.text.format.Formatter

class FileItemPresenter(
    private val accessClickAction: (Context, FileItem, Int, List<FileItem>) -> ActionEntry?,
    var loadCover: Boolean = false,
) : DisplayPresenter<FileItem> {

    override fun clickAction(context: Context, item: FileItem, position: Int, list: List<FileItem>): ActionEntry? =
        accessClickAction(context, item, position, list)

    override fun getIcon(context: Context, item: FileItem): Drawable =
        context.getTintedDrawable(
            if (item.isFolder) R.drawable.ic_folder_white_24dp else R.drawable.ic_file_music_white_24dp,
            themeIconColor(context),
        )!!

    override fun loadImageInto(context: Context, item: FileItem, target: Target): Disposable? =
        if (item.content is FileItem.SongContent && loadCover) {
            loadImage(context)
                .from(item)
                .default(getIcon(context, item))
                .into(target)
                .enqueue()
        } else {
            target.onSuccess(getIcon(context, item))
            null
        }

    override fun menuEntries(context: Context, item: FileItem, position: Int): List<ActionEntry> =
        ActionMenus.FileItem(context, item)

    override fun getItemID(item: FileItem): Long =
        item.mediaPath.hashCode().toLong() shl 16 + item.name.hashCode() shl 1 + if (item.isFolder) 1 else 0

    override fun getDisplayTitle(context: Context, item: FileItem): CharSequence = item.name

    override fun getSecondaryText(context: Context, item: FileItem): CharSequence =
        when (val content = item.content) {
            is FileItem.FolderContent   -> songCountString(context.resources, content.count)
            is FileItem.PlaylistContent -> songCountString(context.resources, content.playlist.size)
            else                        -> Formatter.formatFileSize(context, item.size)
        }

    override fun getTertiaryText(context: Context, item: FileItem): CharSequence? =
        dateTimeText(item.dateModified / 1000)

    override fun getDescription(context: Context, item: FileItem): CharSequence? =
        getSecondaryText(context, item)

    private fun songCountString(resources: Resources, songCount: Int): String {
        return if (songCount >= 0) {
            resources.getQuantityString(R.plurals.item_songs, songCount, songCount)
        } else {
            resources.getString(R.string.label_folder)
        }
    }

}