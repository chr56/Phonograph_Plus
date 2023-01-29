/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.util

import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import android.content.Context
import android.content.Context.MODE_PRIVATE

class QueuePreferenceManager(context: Context) {

    private val preferenceManager =
        context.applicationContext.getSharedPreferences(NAME, MODE_PRIVATE)

    var shuffleMode: ShuffleMode
        get() = preferenceManager.getInt(KEY_SHUFFLE_MODE, 0).let {
            ShuffleMode.deserialize(it)
        }
        set(value) {
            preferenceManager.edit().putInt(KEY_SHUFFLE_MODE, value.serialize()).apply()
        }

    var repeatMode: RepeatMode
        get() = preferenceManager.getInt(KEY_REPEAT_MODE, 0).let {
            RepeatMode.deserialize(it)
        }
        set(value) {
            preferenceManager.edit().putInt(KEY_REPEAT_MODE, value.serialize()).apply()
        }

    var currentPosition: Int
        get() = preferenceManager.getInt(KEY_CURRENT_POSITION, 0)
        set(value) {
            preferenceManager.edit().putInt(KEY_CURRENT_POSITION, value).apply()
        }

    var currentMillisecond: Int
        get() = preferenceManager.getInt(KEY_CURRENT_MILLISECOND, -1)
        set(value) {
            preferenceManager.edit().putInt(KEY_CURRENT_MILLISECOND, value).apply()
        }

    companion object {
        const val NAME = "queue_manager_cfg"
        const val KEY_CURRENT_POSITION = "CURRENT_POSITION"
        const val KEY_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val KEY_REPEAT_MODE = "REPEAT_MODE"
        const val KEY_CURRENT_MILLISECOND = "CURRENT_MILLISECOND"
    }
}

