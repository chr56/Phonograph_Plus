/*
 * Copyright (c) 2021-2022 chr_56
 */

package player.phonograph

object MusicServiceMsgConst {
    private const val PKG_ABBR = App.ACTUAL_PACKAGE_NAME

    // do not change these three strings as it will break support with other apps (e.g. last.fm scrobbling)
    const val REPEAT_MODE_CHANGED = "$PKG_ABBR.repeatmodechanged"
    const val SHUFFLE_MODE_CHANGED = "$PKG_ABBR.shufflemodechanged"
    const val MEDIA_STORE_CHANGED = "$PKG_ABBR.mediastorechanged"

    const val META_CHANGED = "$PKG_ABBR.metachanged"
    const val QUEUE_CHANGED = "$PKG_ABBR.queuechanged"
    const val PLAY_STATE_CHANGED = "$PKG_ABBR.playstatechanged"
}

const val ISSUE_TRACKER_LINK = "https://github.com/chr56/Phonograph_Plus/issues"

const val VERSION_INFO = "versionInfo"
const val UPGRADABLE = "upgradable"
const val NOTIFICATION_CHANNEL_ID_UPGRADABLE = "upgrade_notification"
const val NOTIFICATION_ID_UPGRADABLE = 8747233



const val BROADCAST_PLAYLISTS_CHANGED = "${App.PACKAGE_NAME}.playlists_changed"
