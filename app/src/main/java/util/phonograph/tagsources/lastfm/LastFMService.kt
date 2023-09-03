/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("SpellCheckingInspection")

package util.phonograph.tagsources.lastfm

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
interface LastFMService {

    @GET("$BASE_QUERY_PARAMETERS&method=track.getinfo")
    fun getTrackInfo(
        @Query("track") name: String?,
        @Query("artist") artistName: String?,
        @Query("lang") language: String?,
    ): Call<LastFmTrackResponse?>

    @GET("$BASE_QUERY_PARAMETERS&method=album.getinfo")
    fun getAlbumInfo(
        @Query("album") albumName: String?,
        @Query("artist") artistName: String?,
        @Query("lang") language: String?
    ): Call<LastFmAlbumResponse?>

    @GET("$BASE_QUERY_PARAMETERS&method=artist.getinfo")
    fun getArtistInfo(
        @Query("artist") artistName: String?,
        @Query("lang") language: String?,
        @Header("Cache-Control") cacheControl: String?
    ): Call<LastFmArtistResponse?>


    @GET("${BASE_QUERY_PARAMETERS}&method=album.search")
    fun searchAlbum(
        @Query("album") name: String,
        @Query("page") page: Int,
    ): Call<LastFmSearchResultResponse?>

    @GET("${BASE_QUERY_PARAMETERS}&method=artist.search")
    fun searchArtist(
        @Query("artist") name: String,
        @Query("page") page: Int,
    ): Call<LastFmSearchResultResponse?>

    @GET("${BASE_QUERY_PARAMETERS}&method=track.search")
    fun searchTrack(
        @Query("track") name: String,
        @Query("artist") artist: String?,
        @Query("page") page: Int,
    ): Call<LastFmSearchResultResponse?>

    companion object {
        private const val API_KEY = "bd9c6ea4d55ec9ed3af7d276e5ece304"
        const val BASE_QUERY_PARAMETERS = "?format=json&autocorrect=1&api_key=$API_KEY"
    }
}
