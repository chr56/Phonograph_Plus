/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.loader

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.room.Converters
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.SongEntity

sealed class BaseLoader {
    protected val db get() = MusicDatabase.songsDataBase

    private fun convertSong(song: SongEntity): Song = Converters.toSongModel(song)
    protected fun List<SongEntity>.convertSongs(): List<Song> = map(this@BaseLoader::convertSong)
    protected fun SongEntity?.convert(): Song = this?.let(this@BaseLoader::convertSong) ?: Song.EMPTY_SONG

    private fun convertAlbum(album: AlbumEntity): Album = Converters.toAlbumModel(album)
    protected fun List<AlbumEntity>.convertAlbums(): List<Album> = map(::convertAlbum)
    protected fun AlbumEntity?.convert(): Album = this?.let(::convertAlbum) ?: Album()

    private fun convertArtist(artist: ArtistEntity): Artist = Converters.toArtistModel(artist)
    protected fun List<ArtistEntity>.convertArtists(): List<Artist> = map(this@BaseLoader::convertArtist)
    protected fun ArtistEntity?.convert(): Artist = this?.let(this@BaseLoader::convertArtist) ?: Artist()
}
