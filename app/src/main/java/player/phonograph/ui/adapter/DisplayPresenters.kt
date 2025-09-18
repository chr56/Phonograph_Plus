/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import coil.target.Target
import lib.storage.extension.rootDirectory
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.util.text.albumCountString
import player.phonograph.util.text.dateTextShortText
import player.phonograph.util.text.infoString
import player.phonograph.util.text.makeSectionName
import player.phonograph.util.text.readableDuration
import player.phonograph.util.text.readableYear
import player.phonograph.util.text.songCountString
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.storage.StorageManager

abstract class SongBasicDisplayPresenter(
    private val sortMode: SortMode,
) : DisplayPresenter<Song> {

    override fun getItemID(item: Song): Long = item.id

    override fun getDisplayTitle(context: Context, item: Song): CharSequence = item.title

    override fun getDescription(context: Context, item: Song): CharSequence = item.infoString()
    override fun getSecondaryText(context: Context, item: Song): CharSequence = item.albumName ?: "N/A"
    override fun getTertiaryText(context: Context, item: Song): CharSequence? = item.artistName

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Song> =
        ClickActionProviders.SongClickActionProvider()

    override val menuProvider: ActionMenuProviders.ActionMenuProvider<Song> =
        ActionMenuProviders.SongActionMenuProvider(showPlay = false)

    override fun getIcon(context: Context, item: Song): Drawable? =
        AppCompatResources.getDrawable(context, R.drawable.default_album_art)

    override fun getSortOrderKey(context: Context): SortMode = sortMode

    override fun getSortOrderReference(item: Song, sortMode: SortMode): String? =
        when (sortMode.sortRef) {
            SortRef.SONG_NAME         -> makeSectionName(item.title)
            SortRef.ARTIST_NAME       -> makeSectionName(item.artistName)
            SortRef.ALBUM_NAME        -> makeSectionName(item.albumName)
            SortRef.ALBUM_ARTIST_NAME -> makeSectionName(item.albumArtistName)
            SortRef.COMPOSER          -> makeSectionName(item.composer)
            SortRef.YEAR              -> readableYear(item.year)
            SortRef.DURATION          -> readableDuration(item.duration)
            SortRef.MODIFIED_DATE     -> dateTextShortText(item.dateModified)
            SortRef.ADDED_DATE        -> dateTextShortText(item.dateAdded)
            else                      -> ""
        }

    override fun startLoadingImage(
        context: Context,
        item: Song,
        target: Target,
    ): Disposable? = loadImage(context)
        .from(item)
        .withPalette()
        .default(R.drawable.default_album_art)
        .into(target)
        .enqueue()
}


abstract class AlbumBasicDisplayPresenter(
    private val sortMode: SortMode,
) : DisplayPresenter<Album> {
    override fun getItemID(item: Album): Long = item.id

    override fun getDisplayTitle(context: Context, item: Album): CharSequence = item.title

    override fun getDescription(context: Context, item: Album): CharSequence = item.infoString(context)
    override fun getSecondaryText(context: Context, item: Album): CharSequence = item.artistName ?: "N/A"
    override fun getTertiaryText(context: Context, item: Album): CharSequence = songCountString(context, item.songCount)

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Album> =
        ClickActionProviders.AlbumClickActionProvider()

    override val menuProvider: ActionMenuProviders.ActionMenuProvider<Album> =
        ActionMenuProviders.AlbumActionMenuProvider

    override fun getSortOrderKey(context: Context): SortMode = sortMode

    override fun getIcon(context: Context, item: Album): Drawable? =
        AppCompatResources.getDrawable(context, R.drawable.default_album_art)

    override fun getSortOrderReference(item: Album, sortMode: SortMode): String? =
        when (sortMode.sortRef) {
            SortRef.ALBUM_NAME  -> makeSectionName(item.title)
            SortRef.ARTIST_NAME -> makeSectionName(item.artistName)
            SortRef.YEAR        -> readableYear(item.year)
            SortRef.SONG_COUNT  -> item.songCount.toString()
            else                -> ""
        }

    override fun startLoadingImage(
        context: Context,
        item: Album,
        target: Target,
    ): Disposable? = loadImage(context)
        .from(item)
        .withPalette()
        .default(R.drawable.default_album_art)
        .into(target)
        .enqueue()
}


abstract class ArtistBasicDisplayPresenter(
    private val sortMode: SortMode,
) : DisplayPresenter<Artist> {

    override fun getItemID(item: Artist): Long = item.id

    override fun getDisplayTitle(context: Context, item: Artist): CharSequence = item.name

    override fun getDescription(context: Context, item: Artist): CharSequence = item.infoString(context)

    override fun getSecondaryText(context: Context, item: Artist): CharSequence =
        albumCountString(context, item.albumCount)

    override fun getTertiaryText(context: Context, item: Artist): CharSequence =
        songCountString(context, item.songCount)

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Artist> =
        ClickActionProviders.ArtistClickActionProvider()

    override val menuProvider: ActionMenuProviders.ActionMenuProvider<Artist> =
        ActionMenuProviders.ArtistActionMenuProvider

    override fun getSortOrderKey(context: Context): SortMode = sortMode

    override fun getIcon(context: Context, item: Artist): Drawable? =
        AppCompatResources.getDrawable(context, R.drawable.default_artist_image)

    override fun getSortOrderReference(item: Artist, sortMode: SortMode): String? =
        when (sortMode.sortRef) {
            SortRef.ARTIST_NAME -> makeSectionName(item.name)
            SortRef.ALBUM_COUNT -> item.albumCount.toString()
            SortRef.SONG_COUNT  -> item.songCount.toString()
            else                -> ""
        }

    override fun startLoadingImage(
        context: Context,
        item: Artist,
        target: Target,
    ): Disposable? = loadImage(context)
        .from(item)
        .withPalette()
        .default(R.drawable.default_artist_image)
        .into(target)
        .enqueue()

}

abstract class GenreBasicDisplayPresenter(
    private val sortMode: SortMode,
) : DisplayPresenter<Genre> {

    override fun getItemID(item: Genre): Long = item.id
    override fun getDisplayTitle(context: Context, item: Genre): CharSequence = item.name ?: "UNKNOWN GENRE ${item.id}"
    override fun getDescription(context: Context, item: Genre): CharSequence = item.infoString(context)
    override fun getSecondaryText(context: Context, item: Genre): CharSequence = item.infoString(context)

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Genre> =
        ClickActionProviders.GenreClickActionProvider()

    override val menuProvider: ActionMenuProviders.ActionMenuProvider<Genre> =
        ActionMenuProviders.GenreActionMenuProvider

    override fun getSortOrderKey(context: Context): SortMode = sortMode

    override fun getSortOrderReference(item: Genre, sortMode: SortMode): String? =
        when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME -> makeSectionName(item.name)
            SortRef.SONG_COUNT   -> item.songCount.toString()
            else                 -> ""
        }
}


abstract class PlaylistBasicDisplayPresenter(
    private val sortMode: SortMode,
) : DisplayPresenter<Playlist> {

    override fun getItemID(item: Playlist): Long = item.id
    override fun getDisplayTitle(context: Context, item: Playlist): CharSequence = item.name
    override fun getDescription(context: Context, item: Playlist): CharSequence = item.location.text(context)
    override fun getSecondaryText(context: Context, item: Playlist): CharSequence = item.location.text(context)

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Playlist> =
        ClickActionProviders.PlaylistClickActionProvider()

    override val menuProvider: ActionMenuProviders.ActionMenuProvider<Playlist>?
        get() = ActionMenuProviders.PlaylistActionMenuProvider


    override fun getSortOrderKey(context: Context): SortMode = sortMode

    override fun getSortOrderReference(item: Playlist, sortMode: SortMode): String? =
        when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME  -> makeSectionName(item.name)
            SortRef.MODIFIED_DATE -> dateTextShortText(item.dateModified)
            SortRef.ADDED_DATE    -> dateTextShortText(item.dateAdded)
            else                  -> ""
        }

    override fun getIcon(context: Context, item: Playlist): Drawable? {
        return AppCompatResources.getDrawable(context, item.iconRes)
    }

}

abstract class SongCollectionBasicDisplayPresenter(
    private val sortMode: SortMode,
) : DisplayPresenter<SongCollection> {

    override fun getItemID(item: SongCollection): Long = item.hashCode().toLong()

    override fun getDisplayTitle(context: Context, item: SongCollection): CharSequence = item.name

    override fun getDescription(context: Context, item: SongCollection): CharSequence =
        "${songCountString(context, item.songs.size)} ...${stripStorageVolume(item.detail.orEmpty())}"

    override fun getSecondaryText(context: Context, item: SongCollection): CharSequence =
        stripStorageVolume(item.detail.orEmpty())

    override fun getTertiaryText(context: Context, item: SongCollection): CharSequence? =
        songCountString(context, item.songs.size)

    override fun getSortOrderKey(context: Context): SortMode = sortMode

    override fun getSortOrderReference(item: SongCollection, sortMode: SortMode): String? =
        when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME -> item.name
            else                 -> ""
        }

    override val imageType: Int = DisplayPresenter.IMAGE_TYPE_FIXED_ICON

    override fun getIcon(context: Context, item: SongCollection): Drawable? {
        return AppCompatResources.getDrawable(context, R.drawable.ic_folder_white_24dp)
    }

    companion object {

        private fun stripStorageVolume(str: String): String {
            return str.removePrefix(internalStorageRootPath).removePrefix("/storage")
        }

        private val internalStorageRootPath: String by lazy {
            val storageManager = App.instance.getSystemService<StorageManager>()!!
            val storageVolume = storageManager.primaryStorageVolume
            storageVolume.rootDirectory()?.absolutePath ?: ""
        }
    }
}

abstract class QueueSongBasicDisplayPresenter : DisplayPresenter<QueueSong> {
    override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST
    override val usePalette: Boolean = false
    override val imageType: Int = DisplayPresenter.IMAGE_TYPE_TEXT

    override fun getItemID(item: QueueSong): Long = item.song.id

    override fun getDisplayTitle(context: Context, item: QueueSong) = item.song.title

    override fun getDescription(context: Context, item: QueueSong) = item.song.infoString()
    override fun getSecondaryText(context: Context, item: QueueSong) = item.song.albumName ?: "N/A"
    override fun getTertiaryText(context: Context, item: QueueSong) = item.song.artistName

    override fun getNonSortOrderReference(item: QueueSong): String? = item.index.toString()
    override fun getRelativeOrdinalText(item: QueueSong): String? = item.index.toString()
}