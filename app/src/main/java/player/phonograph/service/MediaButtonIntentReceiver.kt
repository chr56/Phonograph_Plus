/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

// Modified for Phonograph by Karim Abou Zeid (kabouzeid).
package player.phonograph.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import player.phonograph.BuildConfig

/**
 * Used to control headset playback.
 * Single press: pause/resume
 * Double press: next track
 * Triple press: previous track
 */
class MediaButtonIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (DEBUG) Log.v(TAG, "Received intent: $intent")

        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    companion object {
        private val DEBUG = BuildConfig.DEBUG
        const val TAG = "MediaButtonIntentReceiver"

        private const val MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2
        private const val DOUBLE_CLICK = 400

        private var mWakeLock: WakeLock? = null
        private var mClickCounter = 0
        private var mLastClickTime: Long = 0

        @SuppressLint("HandlerLeak") // false alarm, handler is already static
        private val mHandler: Handler = object : Handler(Looper.myLooper() ?: Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_HEADSET_DOUBLE_CLICK_TIMEOUT -> {
                        val clickCount = msg.arg1
                        if (DEBUG) Log.v(TAG, "Handling headset click, count = $clickCount")
                        if (clickCount in 1..3)
                            startService(
                                msg.obj as Context,
                                when (clickCount) {
                                    1 -> MusicService.ACTION_TOGGLE_PAUSE
                                    2 -> MusicService.ACTION_SKIP
                                    3 -> MusicService.ACTION_REWIND
                                    else -> MusicService.ACTION_TOGGLE_PAUSE // impossible
                                }
                            )
                    }
                }
                releaseWakeLockIfHandlerIdle()
            }
        }

        fun handleIntent(context: Context, intent: Intent): Boolean {
            if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
                val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
                val keycode = event.keyCode
                val action = event.action
                val eventTime = if (event.eventTime != 0L) event.eventTime else System.currentTimeMillis()
                // Fallback to system time if event time was not available.
                var command: String? = null
                when (keycode) {
                    KeyEvent.KEYCODE_MEDIA_STOP -> command = MusicService.ACTION_STOP
                    KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                        command = MusicService.ACTION_TOGGLE_PAUSE
                    KeyEvent.KEYCODE_MEDIA_NEXT -> command = MusicService.ACTION_SKIP
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> command = MusicService.ACTION_REWIND
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> command = MusicService.ACTION_PAUSE
                    KeyEvent.KEYCODE_MEDIA_PLAY -> command = MusicService.ACTION_PLAY
                }
                if (command != null) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (event.repeatCount == 0) {
                            // Only consider the first event in a sequence, not the repeat events,
                            // so that we don't trigger in cases where the first event went to
                            // a different app (e.g. when the user ends a phone call by
                            // long pressing the headset button)

                            // The service may or may not be running, but we need to send it
                            // a command.
                            if (keycode == KeyEvent.KEYCODE_HEADSETHOOK || keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                                if (eventTime - mLastClickTime >= DOUBLE_CLICK) {
                                    mClickCounter = 0
                                }
                                mClickCounter++
                                if (DEBUG) Log.v(TAG, "Got headset click, count = $mClickCounter")
                                mHandler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)
                                val msg = mHandler.obtainMessage(
                                    MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0, context
                                )
                                val delay = if (mClickCounter < 3) DOUBLE_CLICK.toLong() else 0.toLong()
                                if (mClickCounter >= 3) {
                                    mClickCounter = 0
                                }
                                mLastClickTime = eventTime
                                acquireWakeLockAndSendMessage(context, msg, delay)
                            } else {
                                startService(context, command)
                            }
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun startService(context: Context, command: String) {
            val intent = Intent(context, MusicService::class.java).apply { action = command }
            try {
                // IMPORTANT NOTE: (kind of a hack)
                // on Android O and above the following crashes when the app is not running
                // there is no good way to check whether the app is running so we catch the exception
                // we do not always want to use startForegroundService() because then one gets an ANR
                // if no notification is displayed via startForeground()
                // according to Play analytics this happens a lot, I suppose for example if command = PAUSE
                context.startService(intent)
            } catch (ignored: IllegalStateException) {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        private fun acquireWakeLockAndSendMessage(context: Context, msg: Message, delay: Long) {
            if (mWakeLock == null) {
                val appContext = context.applicationContext
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                mWakeLock =
                    pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "phonograph:headset_button")
                        .apply { setReferenceCounted(false) }
            }
            if (DEBUG) Log.v(TAG, "Acquiring wake lock and sending " + msg.what)
            // Make sure we don't indefinitely hold the wake lock under any circumstances
            mWakeLock!!.acquire(10000)
            mHandler.sendMessageDelayed(msg, delay)
        }

        private fun releaseWakeLockIfHandlerIdle() {
            if (mHandler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
                if (DEBUG) Log.v(TAG, "Handler still has messages pending, not releasing wake lock")
                return
            }
            mWakeLock?.let {
                it.release()
                mWakeLock = null
                if (DEBUG) Log.v(TAG, "Released wake lock")
            }
        }
    }
}
