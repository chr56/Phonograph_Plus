/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph

const val KEY_STACK_TRACE = "stack_trace"

object PlaylistType {
    const val FILE = 0
    const val ABS_SMART = 1
    const val FAVORITE = 2
    const val LAST_ADDED = 4
    const val HISTORY = 8
    const val MY_TOP_TRACK = 16
    const val RANDOM = 32
}

const val VERSION_INFO = "versionInfo"
const val UPGRADABLE = "upgradable"
const val NOTIFICATION_CHANNEL_ID_UPGRADABLE = "upgrade_notification"
const val NOTIFICATION_ID_UPGRADABLE = 8747233

const val NOTIFICATION_CHANNEL_ID_ERROR = "error_notification"

const val NOTIFICATION_CHANNEL_ID_BACKGROUND = "background_notification"

const val BROADCAST_PLAYLISTS_CHANGED = "${App.PACKAGE_NAME}.playlists_changed"
