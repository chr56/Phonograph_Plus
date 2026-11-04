/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.model.Song
import player.phonograph.model.ui.UIMode

sealed interface PlaylistAction {
    class Load(val scan: Boolean = false) : PlaylistAction
    class Search(val keyword: String) : PlaylistAction
    class UpdateMode(val mode: UIMode) : PlaylistAction

    sealed interface Edit : PlaylistAction {
        class Move(val from: Int, val to: Int) : Edit
        class Delete(val song: Song, val position: Int) : Edit
    }
}