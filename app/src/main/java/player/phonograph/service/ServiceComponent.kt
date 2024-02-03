/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service

interface ServiceComponent {
    fun onCreate(musicService: MusicService)
    fun onDestroy(musicService: MusicService)
}