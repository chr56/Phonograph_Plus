/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import player.phonograph.model.Song

data class PlayRequest(val songs: List<Song>, val position: Int)