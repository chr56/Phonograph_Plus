/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.music_service

import android.app.PendingIntent
import android.content.*
import android.media.AudioManager
import android.os.*
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

    private var _playerController: PlayerController? = null
    private val playerController: PlayerController get() = _playerController!!

    val audioFocusManager: AudioFocusManager = AudioFocusManager()

    private var _handler: MessageHandler? = null
    val messageHandler: Handler get() = _handler!!
    private lateinit var handlerThread: HandlerThread

    override fun onCreate() {
        super.onCreate()
        // init wake lock
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Phonograph:wake_lock")
        wakeLock?.setReferenceCounted(false)

        // init queue manager
        _queueManager = QueueManager(this)
        // init media session callback
        mediaSessionCallback = MediaSessionCallback()
        // init handler
        handlerThread = HandlerThread("music_message_handler_thread")
        handlerThread.start()
        _handler = MessageHandler(handlerThread.looper, this)

        // setup media session
        mediaSession = MediaSessionCompat(
            this,
            MEDIA_SESSION_TAG,
            ComponentName(applicationContext, MediaButtonIntentReceiver::class.java),
            PendingIntent.getBroadcast(
                applicationContext,
                1,
                Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    component = ComponentName(applicationContext, MediaButtonIntentReceiver::class.java)
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        ).apply {
            setPlaybackState(
                PlaybackStateCompat.Builder().setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
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
        _playerController?.destroy()
        _playerController = null
        handlerThread.quitSafely()
        _handler?.looper?.quitSafely()
        _handler?.releaseContext()
        _handler = null
    }

    private fun checkPlayerController() {
        if (_playerController == null) _playerController = PlayerController(this)
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
            checkPlayerController()
            if (audioFocusManager.requestAudioFocus()) {
                playerController.play()
                mediaSession.isActive = true
                // todo update metadata
                // mediaSession.setMetadata()
                // todo notification
                if (!noisyReceiverRegistered) {
                    registerReceiver(becomingNoisyReceiver, becomingNoisyReceiverIntentFilter)
                    noisyReceiverRegistered = true
                }
            }
        }

        override fun onPause() {
            checkPlayerController()
            playerController.pause()
            playerController.pauseReason = PlayerController.PAUSE_BY_MANUAL_ACTION
            // todo update metadata
            // mediaSession.setMetadata()
            audioFocusManager.abandonAudioFocus()
            unregisterReceiver(becomingNoisyReceiver)
        }

        override fun onSkipToNext() {
            checkPlayerController()
            playerController.jumpForward()
            // todo update metadata
            // mediaSession.setMetadata()
        }

        override fun onSkipToPrevious() {
            checkPlayerController()
            playerController.back()
            // todo update metadata
            // mediaSession.setMetadata()
        }

        override fun onSeekTo(pos: Long) {
            checkPlayerController()
            playerController.seek(position = pos)
        }

        override fun onStop() {
            onPause()
            mediaSession.isActive = false
            stopSelf()
        }

        private var noisyReceiverRegistered = false
        private val becomingNoisyReceiverIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    onPause()
                    checkPlayerController()
                    playerController.pauseReason = PlayerController.PAUSE_FOR_AUDIO_BECOMING_NOISY
                }
            }
        }
    }

    private class MessageHandler(looper: Looper, musicService: MusicService) : Handler(looper) {
        private var service: MusicService? = null

        init {
            service = musicService
        }

        fun releaseContext() {
            service = null
        }

        override fun handleMessage(msg: Message) {
        }
    }

    fun isPlaying(): Boolean =
        if (_playerController != null) {
            playerController.isPlaying()
        } else {
            false
        }

    val currentTimeAxis: Int = if (_playerController != null) playerController.currentTimeAxis else -1

    companion object {
        private const val MEDIA_SESSION_TAG = "${App.ACTUAL_PACKAGE_NAME}.MediaSession"
    }
}
