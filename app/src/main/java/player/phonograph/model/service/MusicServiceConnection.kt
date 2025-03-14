/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

import android.content.ServiceConnection

interface MusicServiceConnection : ServiceConnection {
    fun onServiceDetached()
}