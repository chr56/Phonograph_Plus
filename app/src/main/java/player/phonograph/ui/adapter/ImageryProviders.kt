/*
 *  Copyright (c) 2026 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import coil.target.Target
import player.phonograph.R
import player.phonograph.mechanism.coil.loadImage
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.file.FileItem
import player.phonograph.model.playlist.Playlist
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.themeIconColor
import androidx.appcompat.content.res.AppCompatResources
import android.content.Context
import android.graphics.drawable.Drawable

class SongArtwork(override val usePalette: Boolean) : ImageryProvider.Artwork<Song> {

    override fun startLoadingImage(context: Context, item: Song, target: Target): Disposable? =
        loadImage(context)
            .from(item)
            .withPalette()
            .default(R.drawable.default_album_art)
            .into(target)
            .enqueue()
}

class AlbumArtwork(override val usePalette: Boolean) : ImageryProvider.Artwork<Album> {

    override fun startLoadingImage(context: Context, item: Album, target: Target): Disposable? =
        loadImage(context)
            .from(item)
            .withPalette()
            .default(R.drawable.default_album_art)
            .into(target)
            .enqueue()
}

class ArtistArtwork(override val usePalette: Boolean) : ImageryProvider.Artwork<Artist> {

    override fun startLoadingImage(context: Context, item: Artist, target: Target): Disposable? =
        loadImage(context)
            .from(item)
            .withPalette()
            .default(R.drawable.default_artist_image)
            .into(target)
            .enqueue()
}

object PlaylistIcon : ImageryProvider.AdaptiveIcon<Playlist> {

    override fun getIcon(context: Context, item: Playlist): Drawable =
        requireNotNull(AppCompatResources.getDrawable(context, item.iconRes))
}

class FileItemArtwork(private val loadCover: () -> Boolean) : ImageryProvider.Artwork<FileItem> {

    override fun startLoadingImage(context: Context, item: FileItem, target: Target): Disposable? =
        when (item.content) {
            is FileItem.SongContent -> {
                if (loadCover()) {
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
        context.getTintedDrawable(R.drawable.ic_file_music_white_24dp, themeIconColor(context))!!

    private fun iconFolder(context: Context): Drawable =
        context.getTintedDrawable(R.drawable.ic_folder_white_24dp, themeIconColor(context))!!
}
