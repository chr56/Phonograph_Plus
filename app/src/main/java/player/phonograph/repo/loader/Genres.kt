/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.IGenres
import player.phonograph.repo.mediastore.MediaStoreGenres

/**
 * Endpoint for accessing genres
 */
object Genres : IGenres by MediaStoreGenres