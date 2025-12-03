/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.sync

class SyncReport(
    val success: Boolean,
    val added: Int = 0,
    val modified: Int = 0,
    val removed: Int = 0,
    val ignored: Int = 0,
)