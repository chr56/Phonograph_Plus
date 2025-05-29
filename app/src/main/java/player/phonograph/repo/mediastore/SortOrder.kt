/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.AudioColumns

//region Songs
fun defaultSongQuerySortOrder(context: Context) =
    mediastoreSongQuerySortOrder(Setting(context)[Keys.songSortMode].data)

fun mediastoreSongQuerySortOrder(sortMode: SortMode): String =
    "${mediastoreSongQuerySortRef(sortMode.sortRef)} ${if (sortMode.revert) "DESC" else "ASC"}"

private fun mediastoreSongQuerySortRef(sortRef: SortRef): String = when (sortRef) {
    SortRef.ID                -> AudioColumns._ID
    SortRef.SONG_NAME         -> Audio.Media.DEFAULT_SORT_ORDER
    SortRef.ARTIST_NAME       -> Audio.Artists.DEFAULT_SORT_ORDER
    SortRef.ALBUM_NAME        -> Audio.Albums.DEFAULT_SORT_ORDER
    SortRef.ALBUM_ARTIST_NAME -> Audio.Media.ALBUM_ARTIST
    SortRef.COMPOSER          -> Audio.Media.COMPOSER
    SortRef.ADDED_DATE        -> Audio.Media.DATE_ADDED
    SortRef.MODIFIED_DATE     -> Audio.Media.DATE_MODIFIED
    SortRef.DURATION          -> Audio.Media.DURATION
    SortRef.YEAR              -> Audio.Media.YEAR
    SortRef.DISPLAY_NAME      -> Audio.Media.DEFAULT_SORT_ORDER
    SortRef.PATH              -> AudioColumns.DATA
    SortRef.SIZE              -> AudioColumns._ID // invalid
    SortRef.SONG_COUNT        -> AudioColumns._ID // invalid
    SortRef.ALBUM_COUNT       -> AudioColumns._ID // invalid
}
//endregion

//region Albums
fun mediastoreAlbumSortRefKey(sortRef: SortRef): (Album) -> Comparable<*>? =
    when (sortRef) {
        SortRef.ALBUM_NAME  -> { album: Album -> album.title }
        SortRef.ARTIST_NAME -> { album: Album -> album.artistName }
        SortRef.YEAR        -> { album: Album -> album.year }
        SortRef.SONG_COUNT  -> { album: Album -> album.songCount }
        else                -> { album: Album -> null }
    }
//endregion

//region Artists
fun mediastoreArtistSortRefKey(sortRef: SortRef): (Artist) -> Comparable<*>? =
    when (sortRef) {
        SortRef.ARTIST_NAME -> { artist: Artist -> artist.name }
        SortRef.ALBUM_COUNT -> { artist: Artist -> artist.albumCount }
        SortRef.SONG_COUNT  -> { artist: Artist -> artist.songCount }
        else                -> { artist: Artist -> null }
    }
//endregion

//region Genres
fun mediastoreGenreSortRefKey(sortRef: SortRef): (Genre) -> Comparable<*>? =
    when (sortRef) {
        SortRef.DISPLAY_NAME -> { genre: Genre -> genre.name }
        SortRef.SONG_COUNT   -> { genre: Genre -> genre.songCount }
        else                 -> { genre: Genre -> null }
    }
//endregion

//region Playlists
fun mediastorePlaylistSortRefKey(sortRef: SortRef): (Playlist) -> Comparable<*>? =
    when (sortRef) {
        SortRef.DISPLAY_NAME  -> { playlist: Playlist -> playlist.name }
        SortRef.PATH          -> { playlist: Playlist -> playlist.location }
        SortRef.ADDED_DATE    -> { playlist: Playlist -> playlist.dateAdded }
        SortRef.MODIFIED_DATE -> { playlist: Playlist -> playlist.dateModified }
        else                  -> { playlist: Playlist -> null }
    }
//endregion