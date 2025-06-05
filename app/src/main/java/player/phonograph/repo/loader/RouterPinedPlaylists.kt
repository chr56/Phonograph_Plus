/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.IPinedPlaylists
import player.phonograph.repo.room.domain.RoomPinedPlaylists

object RouterPinedPlaylists : IPinedPlaylists by RoomPinedPlaylists