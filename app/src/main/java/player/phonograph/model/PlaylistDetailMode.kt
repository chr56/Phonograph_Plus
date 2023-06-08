/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.model

sealed class PlaylistDetailMode {
    object Common : PlaylistDetailMode()
    object Editor : PlaylistDetailMode()
    object Search : PlaylistDetailMode()
}