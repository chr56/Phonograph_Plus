/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.adapter

import lib.storage.extension.rootDirectory
import player.phonograph.App
import player.phonograph.foundation.dateTextShortText
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.actions.ActionMenus
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.ui.resource.Durations
import player.phonograph.ui.resource.Texts
import player.phonograph.ui.resource.albumCountString
import player.phonograph.ui.resource.buildInfoString
import player.phonograph.ui.resource.infoString
import player.phonograph.ui.resource.readableYear
import player.phonograph.ui.resource.songCountString
import androidx.core.content.getSystemService
import android.content.Context
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class SongDisplayPresenter : DisplayPresenter<Song> {

    override fun getItemID(item: Song): Long = item.id

    override fun getDisplayTitle(context: Context, item: Song): CharSequence = item.title

    override fun getDescription(context: Context, item: Song): CharSequence = item.infoString()
    override fun getSecondaryText(context: Context, item: Song): CharSequence = item.albumName ?: "N/A"
    override fun getTertiaryText(context: Context, item: Song): CharSequence? = item.artistName

    override fun menuEntries(context: Context, item: Song, position: Int): List<ActionEntry> =
        ActionMenus.Song(context, item, showPlay = false)

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Song> =
        ClickActionProviders.SongClickActionProvider()

    override fun getSectionText(item: Song, sortMode: SortMode?): String? =
        when (sortMode?.sortRef) {
            SortRef.SONG_NAME         -> makeSectionName(item.title)
            SortRef.ARTIST_NAME       -> makeSectionName(item.artistName)
            SortRef.ALBUM_NAME        -> makeSectionName(item.albumName)
            SortRef.ALBUM_ARTIST_NAME -> makeSectionName(item.albumArtistName)
            SortRef.COMPOSER          -> makeSectionName(item.composer)
            SortRef.YEAR              -> readableYear(item.year)
            SortRef.DURATION          -> Durations.short(item.duration)
            SortRef.MODIFIED_DATE     -> dateTextShortText(item.dateModified)
            SortRef.ADDED_DATE        -> dateTextShortText(item.dateAdded)
            else                      -> null
        }

    override val imageryProvider: ImageryProvider<Song> = SongArtwork

}

open class AlbumSongDisplayPresenter : SongDisplayPresenter() {

    override val imageryProvider: ImageryProvider<Song> = ImageryProvider.Text

    override fun getSectionText(item: Song, sortMode: SortMode?): String = trackNumber(item)

    override fun getOrdinalText(item: Song, position: Int): String = trackNumber(item)

    override fun getDescription(context: Context, item: Song): CharSequence =
        buildInfoString(Durations.short(item.duration), item.artistName)

    private fun trackNumber(item: Song): String {
        // iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
        val num = item.trackNumber % 1000
        return if (num > 0) num.toString() else "-"
    }
}

open class AlbumDisplayPresenter : DisplayPresenter<Album> {
    override fun getItemID(item: Album): Long = item.id

    override fun getDisplayTitle(context: Context, item: Album): CharSequence = item.title

    override fun getDescription(context: Context, item: Album): CharSequence = item.infoString(context)
    override fun getSecondaryText(context: Context, item: Album): CharSequence = item.artistName ?: "N/A"
    override fun getTertiaryText(context: Context, item: Album): CharSequence = songCountString(context, item.songCount)

    override fun menuEntries(context: Context, item: Album, position: Int): List<ActionEntry> =
        ActionMenus.Composite(context, item) { context, album ->
            withContext(Dispatchers.IO) { Songs.album(context, album.id) }
        }

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Album> =
        ClickActionProviders.AlbumClickActionProvider()

    override fun getSectionText(item: Album, sortMode: SortMode?): String? =
        when (sortMode?.sortRef) {
            SortRef.ALBUM_NAME  -> makeSectionName(item.title)
            SortRef.ARTIST_NAME -> makeSectionName(item.artistName)
            SortRef.YEAR        -> readableYear(item.year)
            SortRef.SONG_COUNT  -> item.songCount.toString()
            else                -> null
        }

    override val imageryProvider: ImageryProvider<Album> = AlbumArtwork
}

open class ArtistAlbumDisplayPresenter : AlbumDisplayPresenter() {
    override fun getDescription(context: Context, item: Album): CharSequence =
        buildInfoString(readableYear(item.year), songCountString(context, item.songCount))
}

open class ArtistDisplayPresenter : DisplayPresenter<Artist> {

    override fun getItemID(item: Artist): Long = item.id

    override fun getDisplayTitle(context: Context, item: Artist): CharSequence = item.name

    override fun getDescription(context: Context, item: Artist): CharSequence = item.infoString(context)

    override fun getSecondaryText(context: Context, item: Artist): CharSequence =
        albumCountString(context, item.albumCount)

    override fun getTertiaryText(context: Context, item: Artist): CharSequence =
        songCountString(context, item.songCount)

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Artist> =
        ClickActionProviders.ArtistClickActionProvider()

    override fun menuEntries(context: Context, item: Artist, position: Int): List<ActionEntry> =
        ActionMenus.Composite(context, item) { context, artist ->
            withContext(Dispatchers.IO) { Songs.artist(context, artist.id) }
        }

    override fun getSectionText(item: Artist, sortMode: SortMode?): String? =
        when (sortMode?.sortRef) {
            SortRef.ARTIST_NAME -> makeSectionName(item.name)
            SortRef.ALBUM_COUNT -> item.albumCount.toString()
            SortRef.SONG_COUNT  -> item.songCount.toString()
            else                -> null
        }

    override val imageryProvider: ImageryProvider<Artist> = ArtistArtwork

}

open class GenreDisplayPresenter : DisplayPresenter<Genre> {

    override fun getItemID(item: Genre): Long = item.id
    override fun getDisplayTitle(context: Context, item: Genre): CharSequence = item.name ?: "UNKNOWN GENRE ${item.id}"
    override fun getDescription(context: Context, item: Genre): CharSequence = item.infoString(context)
    override fun getSecondaryText(context: Context, item: Genre): CharSequence = item.infoString(context)

    override fun menuEntries(context: Context, item: Genre, position: Int): List<ActionEntry> =
        ActionMenus.Composite(context, item) { context, genre ->
            withContext(Dispatchers.IO) { Genres.songs(context, genre.id) }
        }

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Genre> =
        ClickActionProviders.GenreClickActionProvider()

    override fun getSectionText(item: Genre, sortMode: SortMode?): String? =
        when (sortMode?.sortRef) {
            SortRef.DISPLAY_NAME -> makeSectionName(item.name)
            SortRef.SONG_COUNT   -> item.songCount.toString()
            else                 -> null
        }

    override val imageryProvider: ImageryProvider<Genre> = ImageryProvider.None
}

open class PlaylistDisplayPresenter : DisplayPresenter<Playlist> {

    override fun getItemID(item: Playlist): Long = item.id
    override fun getDisplayTitle(context: Context, item: Playlist): CharSequence = item.name
    override fun getDescription(context: Context, item: Playlist): CharSequence =
        Texts.playlist(context.resources, item.location)

    override fun getSecondaryText(context: Context, item: Playlist): CharSequence =
        Texts.playlist(context.resources, item.location)

    override fun menuEntries(context: Context, item: Playlist, position: Int): List<ActionEntry> =
        ActionMenus.Playlist(context, item)

    override val clickActionProvider: ClickActionProviders.ClickActionProvider<Playlist> =
        ClickActionProviders.PlaylistClickActionProvider()


    override fun getSectionText(item: Playlist, sortMode: SortMode?): String? =
        when (sortMode?.sortRef) {
            SortRef.DISPLAY_NAME  -> makeSectionName(item.name)
            SortRef.MODIFIED_DATE -> dateTextShortText(item.dateModified)
            SortRef.ADDED_DATE    -> dateTextShortText(item.dateAdded)
            else                  -> null
        }

    override val imageryProvider: ImageryProvider<Playlist> = PlaylistIcon

}

open class SongCollectionDisplayPresenter(override val clickActionProvider: ClickActionProviders.ClickActionProvider<SongCollection>) : DisplayPresenter<SongCollection> {

    override fun getItemID(item: SongCollection): Long = item.hashCode().toLong()

    override fun getDisplayTitle(context: Context, item: SongCollection): CharSequence = item.name

    override fun getDescription(context: Context, item: SongCollection): CharSequence =
        "${songCountString(context, item.songs.size)} ...${stripStorageVolume(item.detail.orEmpty())}"

    override fun getSecondaryText(context: Context, item: SongCollection): CharSequence =
        stripStorageVolume(item.detail.orEmpty())

    override fun getTertiaryText(context: Context, item: SongCollection): CharSequence? =
        songCountString(context, item.songs.size)

    override fun getSectionText(item: SongCollection, sortMode: SortMode?): String? =
        when (sortMode?.sortRef) {
            SortRef.DISPLAY_NAME -> item.name
            else                 -> null
        }

    override fun menuEntries(context: Context, item: SongCollection, position: Int): List<ActionEntry> =
        ActionMenus.Composite(context, item) { _, collection -> collection.songs }

    override val imageryProvider: ImageryProvider<SongCollection> = ImageryProvider.FixedIcon.FOLDER

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

abstract class QueueSongDisplayPresenter : DisplayPresenter<QueueSong> {

    override fun getItemID(item: QueueSong): Long = item.song.id

    override fun getDisplayTitle(context: Context, item: QueueSong) = item.song.title

    override fun getDescription(context: Context, item: QueueSong) = item.song.infoString()
    override fun getSecondaryText(context: Context, item: QueueSong) = item.song.albumName ?: "N/A"
    override fun getTertiaryText(context: Context, item: QueueSong) = item.song.artistName

    override fun getSectionText(item: QueueSong, sortMode: SortMode?): String? = item.index.toString()
    override fun getOrdinalText(item: QueueSong, position: Int): String? = item.index.toString()

    override val imageryProvider: ImageryProvider<QueueSong> = ImageryProvider.Text
}


private fun makeSectionName(reference: String?): String {
    if (reference.isNullOrBlank()) return ""
    var str = reference.trim { it <= ' ' }.lowercase()
    str = when {
        str.startsWith("the ") -> str.substring(4)
        str.startsWith("a ")   -> str.substring(2)
        else                   -> str
    }
    return if (str.isEmpty()) "" else str[0].uppercase()
}