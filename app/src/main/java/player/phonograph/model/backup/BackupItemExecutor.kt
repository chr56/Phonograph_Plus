/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import okio.Buffer
import okio.Source
import android.content.Context

/**
 * The executor of a [BackupItem]
 */
interface BackupItemExecutor {

    /**
     * Export this [BackupItem] to [Buffer]
     * @return null if failed or empty
     */
    suspend fun export(context: Context): Buffer?

    /**
     * Import  this [BackupItem] from [source]
     * @return true if success
     */
    suspend fun import(context: Context, source: Source): Boolean
}