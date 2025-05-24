/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.repo.room.MusicDatabase

sealed class RoomLoader {
    protected val db get() = MusicDatabase.koinInstance
}
