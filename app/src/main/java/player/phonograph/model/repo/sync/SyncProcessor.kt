/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.sync

import android.content.Context

interface SyncProcessor {
    fun onSyncFirstTime(context: Context)
    fun onSyncTriggered(context: Context)
}