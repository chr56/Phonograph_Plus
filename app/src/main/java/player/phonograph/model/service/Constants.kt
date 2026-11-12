/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

import player.phonograph.ACTUAL_PACKAGE_NAME

private const val PREFIX = ACTUAL_PACKAGE_NAME

//region Action (for Intents)

const val ACTION_TOGGLE_PAUSE = "$PREFIX.togglepause"
const val ACTION_PLAY = "$PREFIX.play"
const val ACTION_PAUSE = "$PREFIX.pause"
const val ACTION_NEXT = "$PREFIX.skip_to_next"
const val ACTION_PREVIOUS = "$PREFIX.skip_to_previous"
const val ACTION_FAST_REWIND = "$PREFIX.fast_rewind"
const val ACTION_FAST_FORWARD = "$PREFIX.fast_forward"
const val ACTION_SHUFFLE = "$PREFIX.toggle_shuffle"
const val ACTION_REPEAT = "$PREFIX.toggle_repeat"
const val ACTION_FAV = "$PREFIX.fav"
const val ACTION_STOP_AND_QUIT_NOW = "$PREFIX.stop_and_quit_now"
const val ACTION_STOP_AND_QUIT_PENDING = "$PREFIX.stop_and_quit_pending"
const val ACTION_CANCEL_PENDING_QUIT = "$PREFIX.cancel_pending_quit"
const val ACTION_CONNECT_WIDGETS = "$PREFIX.connect_widgets"

//endregion


//region Events

const val EVENT_REPEAT_MODE_CHANGED = "$PREFIX.repeatmodechanged"
const val EVENT_SHUFFLE_MODE_CHANGED = "$PREFIX.shufflemodechanged"

// do not change these three strings as it will break support with other apps (e.g. last.fm scrobbling)
const val EVENT_META_CHANGED = "$PREFIX.metachanged"
const val EVENT_QUEUE_CHANGED = "$PREFIX.queuechanged"
const val EVENT_PLAY_STATE_CHANGED = "$PREFIX.playstatechanged"

//endregion
