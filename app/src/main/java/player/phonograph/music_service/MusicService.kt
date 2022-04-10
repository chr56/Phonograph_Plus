/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.music_service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import player.phonograph.App
import player.phonograph.service.MediaButtonIntentReceiver // todo

class MusicService : MediaBrowserServiceCompat() {
    private var wakeLock: WakeLock? = null

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionCallback: MediaSessionCallback

    private var _queueManager: QueueManager? = null
    val queueManager: QueueManager get() = _queueManager!!

    val audioFocusManager: AudioFocusManager = AudioFocusManager()

    override fun onCreate() {
        super.onCreate()
        // init wake lock
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Phonograph:wake_lock")
        wakeLock?.setReferenceCounted(false)

        // init queue manager
        _queueManager = QueueManager(this)
        // init media session callback
        mediaSessionCallback = MediaSessionCallback()

        // setup media session
        mediaSession = MediaSessionCompat(
            this,
            MEDIA_SESSION_TAG,
            ComponentName(applicationContext, MediaButtonIntentReceiver::class.java),
            PendingIntent.getBroadcast(
                applicationContext,
                1,
                Intent(Intent.ACTION_MEDIA_BUTTON).apply { component = ComponentName(applicationContext, MediaButtonIntentReceiver::class.java) },
                PendingIntent.FLAG_IMMUTABLE
            )
        ).apply {
            setPlaybackState(
                PlaybackStateCompat.Builder().setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                ).build()
            )
            setCallback(mediaSessionCallback)
            // mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        }

        // setup notification
        // todo

        // setup session token
        this.sessionToken = mediaSession.sessionToken
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        wakeLock?.release()
        _queueManager?.release()
        _queueManager = null
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // todo
        return BrowserRoot("ROOT", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // todo
        result.sendResult(null)
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            TODO()
        }
        override fun onPause() {
            TODO()
        }
        override fun onSkipToNext() {
            TODO()
        }
        override fun onSkipToPrevious() {
            TODO()
        }
        override fun onSeekTo(pos: Long) {
            TODO()
        }
    }

    companion object {
        private const val MEDIA_SESSION_TAG = "${App.ACTUAL_PACKAGE_NAME}.MediaSession"
    }
}
