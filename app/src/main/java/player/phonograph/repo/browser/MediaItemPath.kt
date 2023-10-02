/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

data class MediaItemPath(val segments: List<String>, val parameters: Map<String, String>?) {

    val mediaId: String
        get() {
            val path = segments.joinToString(separator = "/", prefix = "/")
            val parametersString = parameters?.map { (k, v) -> "$k=$v" }?.joinToString(separator = "&", prefix = "?")
            return if (parametersString == null) {
                path
            } else{
                path + parametersString
            }
        }

    companion object {
        internal const val ROOT_PATH = "/"

        const val PLAY_ALL = "ALL"

        const val SHUFFLE = "shuffle"

        const val SONGS = "SONGS"

        const val SONGS_QUEUE = "SONGS__QUEUE"
        const val SONGS_FAVORITES = "SONGS__FAVORITES"
        const val SONGS_TOP_TRACKS = "SONGS__TOP_TRACKS"
        const val SONGS_LAST_ADDED = "SONGS__LAST_ADDED"
        const val SONGS_HISTORY = "SONGS__HISTORY"

        const val ALBUMS = "ALBUMS"
        const val ARTISTS = "ARTISTS"

        fun song(songId: Long) =
            MediaItemPath(
                listOf(SONGS, songId.toString()),
                null
            )

        fun album(albumId: Long) =
            MediaItemPath(
                listOf(ALBUMS, albumId.toString()),
                null
            )

        fun artist(artistId: Long) =
            MediaItemPath(
                listOf(ARTISTS, artistId.toString()),
                null
            )



        val root = MediaItemPath(listOf(), null)
        val pageSongs = MediaItemPath(listOf(SONGS), null)
        val pageAlbums = MediaItemPath(listOf(ALBUMS), null)
        val pageArtist = MediaItemPath(listOf(ARTISTS), null)
        val pageQueue = MediaItemPath(listOf(SONGS_QUEUE), null)
        val pageFavorites = MediaItemPath(listOf(SONGS_FAVORITES), null)
        val pageTopTracks = MediaItemPath(listOf(SONGS_TOP_TRACKS), null)
        val pageLastAdded = MediaItemPath(listOf(SONGS_LAST_ADDED), null)
        val pageHistory = MediaItemPath(listOf(SONGS_HISTORY), null)



        fun allSongs(shuffle: Boolean) =
            MediaItemPath(
                listOf(SONGS, PLAY_ALL),
                mapOf(SHUFFLE to shuffle.toString())
            )

        fun allAlbumSongs(albumId: Long, shuffle: Boolean) =
            MediaItemPath(
                listOf(ALBUMS, albumId.toString(), PLAY_ALL),
                mapOf(SHUFFLE to shuffle.toString())
            )

        fun allArtistSongs(artistId: Long, shuffle: Boolean) =
            MediaItemPath(
                listOf(ARTISTS, artistId.toString(), PLAY_ALL),
                mapOf(SHUFFLE to shuffle.toString())
            )


        fun queueSong(position: Int) =
            MediaItemPath(
                listOf(SONGS_QUEUE, position.toString()),
                null
            )

        fun allFavoritesSongs(shuffle: Boolean) =
            MediaItemPath(
                listOf(SONGS_FAVORITES, PLAY_ALL),
                mapOf(SHUFFLE to shuffle.toString())
            )

        fun allTopTracks(shuffle: Boolean) =
            MediaItemPath(
                listOf(SONGS_TOP_TRACKS, PLAY_ALL),
                mapOf(SHUFFLE to shuffle.toString())
            )

        fun allLastAdded(shuffle: Boolean) =
            MediaItemPath(
                listOf(SONGS_LAST_ADDED, PLAY_ALL),
                mapOf(SHUFFLE to shuffle.toString())
            )

        fun allHistory(shuffle: Boolean) =
            MediaItemPath(
                listOf(SONGS_HISTORY, PLAY_ALL),
                mapOf(SHUFFLE to shuffle.toString())
            )

    }
}