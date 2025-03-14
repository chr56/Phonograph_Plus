/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

import player.phonograph.ACTUAL_PACKAGE_NAME

// do not change these three strings as it will break support with other apps (e.g. last.fm scrobbling)
const val EVENT_REPEAT_MODE_CHANGED = "${ACTUAL_PACKAGE_NAME}.repeatmodechanged"
const val EVENT_SHUFFLE_MODE_CHANGED = "${ACTUAL_PACKAGE_NAME}.shufflemodechanged"
const val EVENT_MEDIA_STORE_CHANGED = "${ACTUAL_PACKAGE_NAME}.mediastorechanged"

const val EVENT_META_CHANGED = "${ACTUAL_PACKAGE_NAME}.metachanged"
const val EVENT_QUEUE_CHANGED = "${ACTUAL_PACKAGE_NAME}.queuechanged"
const val EVENT_PLAY_STATE_CHANGED = "${ACTUAL_PACKAGE_NAME}.playstatechanged"
