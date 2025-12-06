/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.mediastore.MediaStoreSongs

/**
 * Endpoint for accessing songs
 */
object Songs : ISongs by MediaStoreSongs