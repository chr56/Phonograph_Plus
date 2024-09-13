/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service

import android.content.ServiceConnection

interface MusicServiceConnection : ServiceConnection {
    fun onServiceDetached()
}