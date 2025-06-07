/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.mediastore

import player.phonograph.foundation.compat.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.foundation.compat.MediaStoreCompat
import android.content.ContentUris
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.provider.MediaStore


//region Playlists

/**
 * @param volume MediaStore volume name
 * @return playlists (table) uri in MediaStore
 */
fun mediastoreUriPlaylists(volume: String): Uri =
    MediaStoreCompat.Audio.Playlists.getContentUri(volume)

/**
 * @param volume MediaStore volume name
 * @param playlistId playlist id
 * @return playlist members (table) uri in MediaStore
 */
fun mediastoreUriPlaylistMembers(volume: String, playlistId: Long): Uri =
    MediaStoreCompat.Audio.Playlists.Members.getContentUri(volume, playlistId)

/**
 * @param volume MediaStore volume name
 * @param playlistId playlist id
 * @return playlist (item) uri in MediaStore
 */
fun mediastoreUriPlaylist(volume: String, playlistId: Long): Uri =
    ContentUris.withAppendedId(mediastoreUriPlaylists(volume), playlistId)

/**
 * @return playlists (table) uri in MediaStore at `external` volume
 */
fun mediastoreUriPlaylistsExternal(): Uri =
    mediastoreUriPlaylists(MEDIASTORE_VOLUME_EXTERNAL)

/**
 * @param playlistId playlist id
 * @return playlist members (table) uri in MediaStore
 */
fun mediastoreUriPlaylistMembersExternal(playlistId: Long): Uri =
    mediastoreUriPlaylistMembers(MEDIASTORE_VOLUME_EXTERNAL, playlistId)

/**
 * @param playlistId playlist id
 * @return playlist (item) uri in MediaStore at `external` volume
 */
fun mediastoreUriPlaylistExternal(playlistId: Long): Uri =
    mediastoreUriPlaylist(MEDIASTORE_VOLUME_EXTERNAL, playlistId)

//endregion

//region Songs

/**
 * @param volume MediaStore volume name
 * @return songs (table) uri in MediaStore
 */
fun mediastoreUriSongs(volume: String): Uri =
    MediaStore.Audio.Media.getContentUri(volume)

/**
 * @param volume MediaStore volume name
 * @param songId song id
 * @return song (item) uri in MediaStore
 */
fun mediaStoreUriSong(volume: String, songId: Long): Uri =
    ContentUris.withAppendedId(mediastoreUriSongs(volume), songId)

/**
 * @return songs (table) uri in MediaStore at `external` volume
 */
fun mediastoreUriSongsExternal(): Uri =
    mediastoreUriSongs(MEDIASTORE_VOLUME_EXTERNAL)

/**
 * @param songId song id
 * @return song (item) uri in MediaStore at `external` volume
 */
fun mediaStoreUriSongExternal(songId: Long): Uri =
    mediaStoreUriSong(MEDIASTORE_VOLUME_EXTERNAL, songId)

//endregion


//region Genres

/**
 * @param volume MediaStore volume name
 * @return genres (table) uri in MediaStore
 */
fun mediastoreUriGenres(volume: String): Uri =
    MediaStore.Audio.Genres.getContentUri(volume)

/**
 * @param volume MediaStore volume name
 * @param genreId genre id
 * @return genre members (table) uri in MediaStore
 */
fun mediastoreUriGenreMembers(volume: String, genreId: Long): Uri =
    MediaStore.Audio.Genres.Members.getContentUri(volume, genreId)

/**
 * @param volume MediaStore volume name
 * @param genreId genre id
 * @return genre (item) uri in MediaStore
 */
fun mediastoreUriGenre(volume: String, genreId: Long): Uri =
    ContentUris.withAppendedId(mediastoreUriGenres(volume), genreId)


/**
 * @return genres (table) uri in MediaStore at `external` volume
 */
fun mediastoreUriGenresExternal(): Uri =
    mediastoreUriGenres(MEDIASTORE_VOLUME_EXTERNAL)

/**
 * @param genreId genre id
 * @return genre members (table) uri in MediaStore at `external` volume
 */
fun mediastoreUriGenreMembersExternal(genreId: Long): Uri =
    mediastoreUriGenreMembers(MEDIASTORE_VOLUME_EXTERNAL, genreId)

/**
 * @param genreId genre id
 * @return genre (item) uri in MediaStore at `external` volume
 */
fun mediastoreUriGenreExternal(genreId: Long): Uri =
    mediastoreUriGenre(MEDIASTORE_VOLUME_EXTERNAL, genreId)


//endregion


//region Artists

/**
 * @param volume MediaStore volume name
 * @return artists (table) uri in MediaStore
 */
fun mediastoreUriArtists(volume: String): Uri =
    MediaStore.Audio.Artists.getContentUri(volume)

/**
 * @param volume MediaStore volume name
 * @param artistId artist id
 * @return artist (item) uri in MediaStore
 */
fun mediastoreUriArtist(volume: String, artistId: Long): Uri =
    ContentUris.withAppendedId(mediastoreUriArtists(volume), artistId)

//endregion


//region Albums

/**
 * @param volume MediaStore volume name
 * @return albums (table) uri in MediaStore
 */
fun mediastoreUriAlbums(volume: String): Uri =
    MediaStore.Audio.Albums.getContentUri(volume)

/**
 * @param volume MediaStore volume name
 * @param albumId album id
 * @return album (item) uri in MediaStore
 */
fun mediastoreUriAlbum(volume: String, albumId: Long): Uri =
    ContentUris.withAppendedId(mediastoreUriAlbums(volume), albumId)


fun mediaStoreUriAlbumArt(albumId: Long): Uri =
    ContentUris.withAppendedId(AlbumArtContentUri, albumId)

@Suppress("SpellCheckingInspection", "UseKtx")
private val AlbumArtContentUri: Uri by lazy(LazyThreadSafetyMode.NONE) {
    if (SDK_INT >= Q) MediaStore.AUTHORITY_URI.buildUpon()
        .appendPath(MediaStore.VOLUME_EXTERNAL)
        .appendPath("audio")
        .appendPath("albumart")
        .build()
    else Uri.parse("content://media/external/audio/albumart")
}

//endregion