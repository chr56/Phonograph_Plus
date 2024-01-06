/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

sealed interface PlayRequest {
    data object EmptyRequest : PlayRequest
    data class SongRequest(val song: Song) : PlayRequest
    data class SongsRequest(val songs: List<Song>, val position: Int) : PlayRequest
    data class PlayAtRequest(val position: Int) : PlayRequest

    companion object {
        fun from(songs: List<Song>): PlayRequest {
            return when (songs.size) {
                0    -> EmptyRequest
                1    -> SongRequest(songs.first())
                else -> SongsRequest(songs, 0)
            }
        }
    }
}