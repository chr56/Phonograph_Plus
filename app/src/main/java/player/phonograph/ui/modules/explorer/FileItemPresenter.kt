/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.explorer

import coil.request.Disposable
import coil.target.Target
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.model.file.FileItem
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.util.text.dateTimeText
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.themeIconColor
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.text.format.Formatter

class FileItemPresenter(
    override var clickActionProvider: ClickActionProviders.ClickActionProvider<FileItem>,
    override var menuProvider: ActionMenuProviders.ActionMenuProvider<FileItem>?,
    var loadCover: Boolean = false,
) : DisplayPresenter<FileItem> {

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

    override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST

    override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

    override val usePalette: Boolean = false

    private fun songCountString(resources: Resources, songCount: Int): String {
        return if (songCount >= 0) {
            resources.getQuantityString(R.plurals.item_songs, songCount, songCount)
        } else {
            resources.getString(R.string.label_folder)
        }
    }

    //region Images
    override fun startLoadingImage(
        context: Context,
        item: FileItem,
        target: Target,
    ): Disposable? = when (item.content) {
        is FileItem.SongContent -> {
            if (loadCover) {
                loadImage(context)
                    .from(item)
                    .default(iconFile(context))
                    .into(target)
                    .enqueue()
            } else {
                target.onSuccess(iconFile(context))
                null
            }
        }

        else                    -> {
            target.onSuccess(if (item.isFolder) iconFolder(context) else iconFile(context))
            null
        }
    }

    private fun iconFile(context: Context): Drawable =
        context.getTintedDrawable(R.drawable.ic_file_music_white_24dp, color(context))!!

    private fun iconFolder(context: Context): Drawable =
        context.getTintedDrawable(R.drawable.ic_folder_white_24dp, color(context))!!

    private fun color(context: Context): Int = themeIconColor(context)
    //endregion

}