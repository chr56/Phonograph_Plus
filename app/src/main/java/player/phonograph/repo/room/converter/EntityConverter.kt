/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.converter

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.GenreEntity
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.PlaylistEntity
import androidx.room.TypeConverter

object EntityConverter {

    //region Songs
    @TypeConverter
    fun fromSongModel(song: Song): MediastoreSongEntity = MediastoreSongEntity(
        mediastorId = song.id,
        path = song.data,
        duration = song.duration,
        dateAdded = song.dateAdded,
        dateModified = song.dateModified,
        title = song.title,
        albumId = song.albumId,
        album = song.albumName ?: "",
        artistId = song.artistId,
        artist = song.artistName ?: "",
        year = song.year,
        track = song.trackNumber,
        albumArtist = song.albumArtistName ?: "",
        composer = song.composer ?: "",
    )

    @TypeConverter
    fun toSongModel(entity: MediastoreSongEntity): Song = Song(
        id = entity.mediastorId,
        title = entity.title,
        trackNumber = entity.track,
        year = entity.year,
        duration = entity.duration,
        data = entity.path,
        dateAdded = entity.dateAdded,
        dateModified = entity.dateModified,
        albumId = entity.albumId,
        albumName = entity.album,
        artistId = entity.artistId,
        artistName = entity.artist,
        albumArtistName = entity.albumArtist,
        composer = entity.composer,
    )
    //endregion

    //region Playlist
    @TypeConverter
    fun toPlaylist(playlist: PlaylistEntity): Playlist =
        Playlist(
            name = playlist.name,
            location = DatabasePlaylistLocation(playlist.id),
            dateAdded = playlist.dateAdded,
            dateModified = playlist.dateModified,
            iconRes = R.drawable.ic_queue_music_white_24dp
        )
    //endregion

    //region Album
    @TypeConverter
    fun toAlbumModel(albumEntity: AlbumEntity) = Album(
        id = albumEntity.albumId,
        title = albumEntity.albumName,
        songCount = albumEntity.songCount,
        artistId = albumEntity.artistId,
        artistName = albumEntity.albumArtistName,
        year = albumEntity.year,
        dateModified = albumEntity.dateModified,
    )
    //endregion

    //region Artist
    @TypeConverter
    fun toArtistModel(artistEntity: ArtistEntity) = Artist(
        id = artistEntity.artistId,
        name = artistEntity.artistName,
        albumCount = artistEntity.albumCount,
        songCount = artistEntity.songCount,
    )
    //endregion


    //region Genre
    @TypeConverter
    fun fromGenreModel(genre: Genre) = GenreEntity(
        id = 0, // auto generated
        name = genre.name ?: "",
        mediastoreId = genre.id,
        songCount = genre.songCount,
    )
    @TypeConverter
    fun toGenreModel(genreEntity: GenreEntity) = Genre(
        id = genreEntity.id,
        name = genreEntity.name,
        songCount = genreEntity.songCount,
    )
    //endregion

}