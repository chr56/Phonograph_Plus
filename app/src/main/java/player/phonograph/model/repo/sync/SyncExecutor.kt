/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.sync

import android.content.Context

interface SyncExecutor {

    suspend fun check(context: Context): Boolean

    suspend fun sync(context: Context, channel: SyncProgressConnection?): SyncResult

    interface SyncProgressConnection {

        fun onProcessUpdate(message: String?)

        fun onProcessUpdate(current: Int, total: Int)

        fun onProcessUpdate(current: Int, total: Int, message: String?)
    }
}