@file:Suppress("SpellCheckingInspection")

package util.phonograph.lastfm.rest.service

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import util.phonograph.lastfm.rest.model.LastFmAlbum
import util.phonograph.lastfm.rest.model.LastFmArtist

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
interface LastFMService {

    @GET("$BASE_QUERY_PARAMETERS&method=album.getinfo")
    fun getAlbumInfo(
        @Query("album") albumName: String?,
        @Query("artist") artistName: String?,
        @Query("lang") language: String?
    ): Call<LastFmAlbum?>

    @GET("$BASE_QUERY_PARAMETERS&method=artist.getinfo")
    fun getArtistInfo(
        @Query("artist") artistName: String?,
        @Query("lang") language: String?,
        @Header("Cache-Control") cacheControl: String?
    ): Call<LastFmArtist?>

    companion object {
        private const val API_KEY = "bd9c6ea4d55ec9ed3af7d276e5ece304"
        const val BASE_QUERY_PARAMETERS = "?format=json&autocorrect=1&api_key=$API_KEY"
    }
}
