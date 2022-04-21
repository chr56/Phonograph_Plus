package player.phonograph.loader

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.settings.Setting.Companion.instance

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ArtistLoader {

    fun getAllArtists(context: Context): List<Artist> {
        val songs = getSongs(
            makeSongCursor(context, null, null, sortOrder)
        )
        return splitIntoArtists(AlbumLoader.splitIntoAlbums(songs))
    }

    fun getArtists(context: Context, query: String): List<Artist> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST} LIKE ?", arrayOf("%$query%"), sortOrder)
        )
        return splitIntoArtists(AlbumLoader.splitIntoAlbums(songs))
    }

    fun getArtist(context: Context, artistId: Long): Artist {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), sortOrder)
        )
        return Artist(AlbumLoader.splitIntoAlbums(songs))
    }

    fun splitIntoArtists(albums: List<Album>?): List<Artist> {
        val artists: MutableList<Artist> = ArrayList()
        if (albums != null) {
            for (album in albums) {
                getOrCreateArtist(artists, album.artistId).albums?.toMutableList()?.add(album)
            }
        }
        return artists
    }

    private fun getOrCreateArtist(artists: MutableList<Artist>, artistId: Long): Artist {
        for (artist in artists) {
            if (artist.albums!!.isNotEmpty() && artist.albums[0].songs.isNotEmpty() && artist.albums[0].songs[0].artistId == artistId) {
                return artist
            }
        }
        val artist = Artist()
        artists.add(artist)
        return artist
    }

    val sortOrder: String by lazy {
        instance().artistSortOrder + ", " + instance().artistAlbumSortOrder + ", " + instance().albumSongSortOrder
    }
}
