/*
 * Copyright (c) 2021-2022 chr_56
 */

package player.phonograph

const val ACTUAL_PACKAGE_NAME = BuildConfig.APPLICATION_ID

const val PACKAGE_NAME = "player.phonograph"
const val BROADCAST_PLAYLISTS_CHANGED = "$PACKAGE_NAME.playlists_changed"

const val ISSUE_TRACKER_LINK = "https://github.com/chr56/Phonograph_Plus/issues"

object MusicServiceMsgConst {

    // do not change these three strings as it will break support with other apps (e.g. last.fm scrobbling)
    const val REPEAT_MODE_CHANGED = "$ACTUAL_PACKAGE_NAME.repeatmodechanged"
    const val SHUFFLE_MODE_CHANGED = "$ACTUAL_PACKAGE_NAME.shufflemodechanged"
    const val MEDIA_STORE_CHANGED = "$ACTUAL_PACKAGE_NAME.mediastorechanged"

    const val META_CHANGED = "$ACTUAL_PACKAGE_NAME.metachanged"
    const val QUEUE_CHANGED = "$ACTUAL_PACKAGE_NAME.queuechanged"
    const val PLAY_STATE_CHANGED = "$ACTUAL_PACKAGE_NAME.playstatechanged"
}


const val VERSION_INFO = "versionInfo"
const val UPGRADABLE = "upgradable"


