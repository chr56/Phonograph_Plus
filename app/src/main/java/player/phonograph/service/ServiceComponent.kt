/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service

interface ServiceComponent {
    val created: Boolean
    fun onCreate(musicService: MusicService)
    fun onDestroy(musicService: MusicService)
}