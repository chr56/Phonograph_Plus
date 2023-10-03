/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import player.phonograph.model.Song

sealed interface PlayRequest {
    data object EmptyRequest : PlayRequest
    data class SongRequest(val song: Song) : PlayRequest
    data class SongsRequest(val songs: List<Song>, val index: Int) : PlayRequest
    data class PlayAtRequest(val index: Int) : PlayRequest
}