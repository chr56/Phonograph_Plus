/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.musicbrainz

import util.phonograph.tagsources.util.RestResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicBrainzService {

    object Entity {
        const val ARTIST = "artist"
        const val RELEASE = "release"
        const val RELEASE_GROUP = "release-group"
        const val RECORDING = "recording"
    }


    //region lookup

    @GET("${Entity.ARTIST}/{mbid}?fmt=json")
    fun getArtist(
        @Path("mbid") mbid: String,
        @Query("inc") inc: String = "releases+release-groups+tags",
    ): Call<RestResult<MusicBrainzArtist>?>

    @GET("${Entity.RELEASE}/{mbid}?fmt=json")
    fun getRelease(
        @Path("mbid") mbid: String,
        @Query("inc") inc: String = "artists+release-groups+recordings+labels+genres+tags",
    ): Call<RestResult<MusicBrainzRelease>?>

    @GET("${Entity.RELEASE_GROUP}/{mbid}?fmt=json")
    fun getReleaseGroup(
        @Path("mbid") mbid: String,
        @Query("inc") inc: String = "artists+releases+genres+tags",
    ): Call<RestResult<MusicBrainzReleaseGroup>?>

    @GET("${Entity.RECORDING}/{mbid}?fmt=json")
    fun getRecording(
        @Path("mbid") mbid: String,
        @Query("inc") inc: String = "artists+releases+genres+tags",
    ): Call<RestResult<MusicBrainzRecording>?>

    //endregion


    //region search

    @GET("${Entity.ARTIST}?fmt=json&limit=60")
    fun searchArtist(
        @Query("query") query: String,
        @Query("offset") offset: Int,
    ): Call<RestResult<MusicBrainzSearchResultArtists>?>

    @GET("${Entity.RELEASE}?fmt=json&limit=60")
    fun searchRelease(
        @Query("query") query: String,
        @Query("offset") offset: Int,
    ): Call<RestResult<MusicBrainzSearchResultReleases>?>

    @GET("${Entity.RELEASE_GROUP}?fmt=json&limit=60")
    fun searchReleaseGroup(
        @Query("query") query: String,
        @Query("offset") offset: Int,
    ): Call<RestResult<MusicBrainzSearchResultReleasesGroup>?>

    @GET("${Entity.RECORDING}?fmt=json&limit=60")
    fun searchRecording(
        @Query("query") query: String,
        @Query("offset") offset: Int,
    ): Call<RestResult<MusicBrainzSearchResultRecording>?>

    //endregion


}