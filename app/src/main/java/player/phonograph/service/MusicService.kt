package player.phonograph.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.audiofx.AudioEffect
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import player.phonograph.App
import player.phonograph.App.Companion.ACTUAL_PACKAGE_NAME
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.appwidgets.AppWidgetBig
import player.phonograph.appwidgets.AppWidgetCard
import player.phonograph.appwidgets.AppWidgetClassic
import player.phonograph.appwidgets.AppWidgetSmall
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LrcLyrics
import player.phonograph.model.playlist.Playlist
import player.phonograph.provider.HistoryStore
import player.phonograph.provider.SongPlayCountStore
import player.phonograph.service.notification.PlayingNotificationManger
import player.phonograph.service.player.MSG_NOW_PLAYING_CHANGED
import player.phonograph.service.player.PlayerController
import player.phonograph.service.player.PlayerState
import player.phonograph.service.player.PlayerStateObserver
import player.phonograph.service.queue.*
import player.phonograph.service.util.MediaButtonIntentReceiver
import player.phonograph.service.util.MediaStoreObserverUtil
import player.phonograph.service.util.MusicServiceUtil
import player.phonograph.service.util.SongPlayCountHelper
import player.phonograph.settings.Setting

/**
 * @author Karim Abou Zeid (kabouzeid), Andrew Neal
 */
class MusicService : Service(), OnSharedPreferenceChangeListener {

    private val songPlayCountHelper = SongPlayCountHelper()

    var pendingQuit = false // todo sleeptimer

    val queueManager: QueueManager get() = App.instance.queueManager
    private val queueChangeObserver: QueueChangeObserver = initQueueChangeObserver()

    private lateinit var controller: PlayerController
    private var playerStateObserver: PlayerStateObserver = initPlayerStateObserver()

    private lateinit var playNotificationManager: PlayingNotificationManger

    private lateinit var throttledTimer: ThrottledTimer

    private lateinit var uiThreadHandler: Handler

    private val mediaStoreObserverUtil = MediaStoreObserverUtil()

    override fun onCreate() {
        super.onCreate()

        // controller
        controller = PlayerController(this)
        controller.restoreIfNecessary()

        // observers & messages
        sendChangeInternal(META_CHANGED) // notify manually for first setting up queueManager
        sendChangeInternal(QUEUE_CHANGED) // notify manually for first setting up queueManager
        queueManager.addObserver(queueChangeObserver)
        controller.addObserver(playerStateObserver)

        // notifications & media session
        playNotificationManager = PlayingNotificationManger(this)
        playNotificationManager.setupMediaSession(initMediaSessionCallback())
        playNotificationManager.setUpNotification()
        playNotificationManager.mediaSession.isActive = true

        // process updater
        throttledTimer = ThrottledTimer(controller.handler)

        // misc
        uiThreadHandler = Handler(Looper.getMainLooper())
        mediaStoreObserverUtil.setUpMediaStoreObserver(
            this,
            controller.handler, // todo use other handler
            this@MusicService::handleAndSendChangeInternal
        )
        registerReceiver(widgetIntentReceiver, IntentFilter(APP_WIDGET_UPDATE))
        Setting.instance().registerOnSharedPreferenceChangedListener(this)
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_CREATED"))
    }

    private fun initQueueChangeObserver(): QueueChangeObserver = object : QueueChangeObserver {
        override fun onQueueCursorChanged(newPosition: Int) {
            notifyChange(META_CHANGED)
        }

        override fun onQueueChanged(newPlayingQueue: List<Song>, newOriginalQueue: List<Song>) {
            notifyChange(QUEUE_CHANGED)
            notifyChange(META_CHANGED)
        }

        override fun onShuffleModeChanged(newMode: ShuffleMode) {
            notifyChange(SHUFFLE_MODE_CHANGED)
        }

        override fun onRepeatModeChanged(newMode: RepeatMode) {
            controller.handler.removeMessages(
                PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
            )
            controller.handler.sendEmptyMessage(
                PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
            )
            notifyChange(REPEAT_MODE_CHANGED)
        }
    }

    private fun initPlayerStateObserver(): PlayerStateObserver = object : PlayerStateObserver {
        override fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {
            notifyChange(PLAY_STATE_CHANGED)
        }

        override fun onReceivingMessage(msg: Int) {
            when (msg) {
                MSG_NOW_PLAYING_CHANGED -> notifyChange(META_CHANGED)
            }
        }
    }

    private fun initMediaSessionCallback() = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            play()
        }

        override fun onPause() {
            pause()
        }

        override fun onSkipToNext() {
            playNextSong(true)
        }

        override fun onSkipToPrevious() {
            back(true)
        }

        override fun onStop() {
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            seek(pos.toInt())
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            return MediaButtonIntentReceiver.handleIntent(this@MusicService, mediaButtonEvent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action != null) {
                controller.restoreIfNecessary()
                when (intent.action) {
                    ACTION_TOGGLE_PAUSE -> if (isPlaying) {
                        pause()
                    } else {
                        play()
                    }
                    ACTION_PAUSE -> pause()
                    ACTION_PLAY -> play()
                    ACTION_PLAY_PLAYLIST -> parsePlaylistAndPlay(intent, this)
                    ACTION_REWIND -> back(true)
                    ACTION_SKIP -> playNextSong(true)
                    ACTION_STOP, ACTION_QUIT -> {
                        pendingQuit = false
                        stopSelf()
                    }
                    ACTION_PENDING_QUIT -> pendingQuit = true
                }
            }
        }
        return START_NOT_STICKY
    }

    val isPlaying: Boolean get() = controller.isPlaying()

    var isDestroyed = false
        private set

    override fun onDestroy() {
        isDestroyed = true
        playNotificationManager.mediaSession.isActive = false
        playNotificationManager.removeNotification()
        closeAudioEffectSession()
        playNotificationManager.mediaSession.release()
        unregisterReceiver(widgetIntentReceiver)
        mediaStoreObserverUtil.unregisterMediaStoreObserver(this)
        Setting.instance.unregisterOnSharedPreferenceChangedListener(this)
        controller.stopAndDestroy()
        controller.removeObserver(playerStateObserver)
        queueManager.removeObserver(queueChangeObserver)
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_DESTROYED"))
    }

    // todo
    private fun closeAudioEffectSession() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, controller.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    fun runOnUiThread(runnable: Runnable) {
        uiThreadHandler.post(runnable)
    }

    fun openQueue(playingQueue: List<Song>?, startPosition: Int, startPlaying: Boolean) {
        if (playingQueue != null && playingQueue.isNotEmpty() && startPosition >= 0 && startPosition < playingQueue.size) {
            queueManager.swapQueue(playingQueue, startPosition, false)
            if (startPlaying) playSongAt(queueManager.currentSongPosition)
        }
    }

    fun playSongAt(position: Int) = controller.playAt(position)
    fun pause() = controller.pause()
    fun play() = controller.play()
    fun playPreviousSong(force: Boolean) = controller.jumpBackward(force)
    fun back(force: Boolean) = controller.back(force)
    fun playNextSong(force: Boolean) = controller.jumpForward(force)
    val songProgressMillis: Int get() = controller.getSongProgressMillis()
    val songDurationMillis: Int get() = controller.getSongDurationMillis()

    fun seek(millis: Int): Int = synchronized(this) {
        return try {
            val newPosition = controller.seekTo(millis.toLong())
            throttledTimer.notifySeek()
            newPosition
        } catch (e: Exception) {
            -1
        }
    }

    val audioSessionId: Int get() = controller.audioSessionId
    val mediaSession get() = playNotificationManager.mediaSession

    private fun notifyChange(what: String) {
        handleAndSendChangeInternal(what)
        MusicServiceUtil.sendPublicIntent(this, what)
    }

    private fun handleAndSendChangeInternal(what: String) {
        handleChangeInternal(what)
        sendChangeInternal(what)
    }

    private fun sendChangeInternal(what: String) {
        sendBroadcast(Intent(what))
        notifyWidget(what)
    }

    private fun handleChangeInternal(what: String) {
        when (what) {
            PLAY_STATE_CHANGED -> {
                playNotificationManager.updateNotification()
                playNotificationManager.updateMediaSessionPlaybackState()
                val isPlaying = isPlaying
                if (!isPlaying && songProgressMillis > 0) {
                    controller.saveCurrentMills()
                }
                songPlayCountHelper.notifyPlayStateChanged(isPlaying)
            }
            META_CHANGED -> {
                playNotificationManager.updateNotification()
                playNotificationManager.updateMediaSessionMetaData()
                queueManager.postMessage(QueueManager.MSG_SAVE_CURSOR)
                controller.saveCurrentMills()
                val currentSong = queueManager.currentSong
                HistoryStore.getInstance(this).addSongId(currentSong.id)
                if (songPlayCountHelper.shouldBumpPlayCount()) {
                    SongPlayCountStore.getInstance(this).bumpPlayCount(songPlayCountHelper.song.id)
                }
                songPlayCountHelper.notifySongChanged(currentSong)
            }
            QUEUE_CHANGED -> {
                playNotificationManager.updateMediaSessionMetaData() // because playing queue size might have changed
                queueManager.postMessage(QueueManager.MSG_SAVE_QUEUE)
                queueManager.postMessage(QueueManager.MSG_SAVE_CURSOR)
                if (queueManager.playingQueue.isNotEmpty()) {
                    controller.handler.removeMessages(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                    controller.handler.sendEmptyMessage(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                } else {
                    controller.stop()
                    playNotificationManager.removeNotification()
                }
            }
        }
    }

    private val widgetIntentReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val command = intent.getStringExtra(EXTRA_APP_WIDGET_NAME)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                when (command) {
                    AppWidgetClassic.NAME -> {
                        AppWidgetClassic.instance.performUpdate(this@MusicService, ids)
                    }
                    AppWidgetSmall.NAME -> {
                        AppWidgetSmall.instance.performUpdate(this@MusicService, ids)
                    }
                    AppWidgetBig.NAME -> {
                        AppWidgetBig.instance.performUpdate(this@MusicService, ids)
                    }
                    AppWidgetCard.NAME -> {
                        AppWidgetCard.instance.performUpdate(this@MusicService, ids)
                    }
                }
            }
        }

    private fun notifyWidget(what: String) {
        AppWidgetBig.instance.notifyChange(this, what)
        AppWidgetClassic.instance.notifyChange(this, what)
        AppWidgetSmall.instance.notifyChange(this, what)
        AppWidgetCard.instance.notifyChange(this, what)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            Setting.GAPLESS_PLAYBACK -> // todo
                if (sharedPreferences.getBoolean(key, false)) {
                    controller.handler.removeMessages(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                    controller.handler.sendEmptyMessage(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                } else {
                    controller.handler.removeMessages(
                        PlayerController.ControllerHandler.CLEAN_NEXT_PLAYER
                    )
                    controller.handler.sendEmptyMessage(
                        PlayerController.ControllerHandler.CLEAN_NEXT_PLAYER
                    )
                }
            Setting.ALBUM_ART_ON_LOCKSCREEN, Setting.BLURRED_ALBUM_ART -> playNotificationManager.updateMediaSessionMetaData()
            Setting.COLORED_NOTIFICATION -> playNotificationManager.updateNotification()
            Setting.CLASSIC_NOTIFICATION -> {
                playNotificationManager.setUpNotification()
                playNotificationManager.updateNotification()
            }
        }
    }

    fun replaceLyrics(lyrics: LrcLyrics?) = controller.replaceLyrics(lyrics)

    private inner class ThrottledTimer(private val mHandler: Handler) : Runnable {
        fun notifySeek() {
            playNotificationManager.updateMediaSessionMetaData()
            playNotificationManager.updateMediaSessionPlaybackState()
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, THROTTLE)
        }

        override fun run() {
            controller.saveCurrentMills()
            MusicServiceUtil.sendPublicIntent(this@MusicService, PLAY_STATE_CHANGED) // for musixmatch synced lyrics
        }
    }

    @Suppress("SpellCheckingInspection")
    companion object {
        const val ACTION_TOGGLE_PAUSE = "$ACTUAL_PACKAGE_NAME.togglepause"
        const val ACTION_PLAY = "$ACTUAL_PACKAGE_NAME.play"
        const val ACTION_PLAY_PLAYLIST = "$ACTUAL_PACKAGE_NAME.play.playlist"
        const val ACTION_PAUSE = "$ACTUAL_PACKAGE_NAME.pause"
        const val ACTION_STOP = "$ACTUAL_PACKAGE_NAME.stop"
        const val ACTION_SKIP = "$ACTUAL_PACKAGE_NAME.skip"
        const val ACTION_REWIND = "$ACTUAL_PACKAGE_NAME.rewind"
        const val ACTION_QUIT = "$ACTUAL_PACKAGE_NAME.quitservice"
        const val ACTION_PENDING_QUIT = "$ACTUAL_PACKAGE_NAME.pendingquitservice"

        const val INTENT_EXTRA_PLAYLIST = ACTUAL_PACKAGE_NAME + "intentextra.playlist"
        const val INTENT_EXTRA_SHUFFLE_MODE = "$ACTUAL_PACKAGE_NAME.intentextra.shufflemode"

        const val APP_WIDGET_UPDATE = "$ACTUAL_PACKAGE_NAME.appwidgetupdate"
        const val EXTRA_APP_WIDGET_NAME = ACTUAL_PACKAGE_NAME + "app_widget_name"

        // do not change these three strings as it will break support with other apps (e.g. last.fm scrobbling)
        const val META_CHANGED = "$ACTUAL_PACKAGE_NAME.metachanged"
        const val QUEUE_CHANGED = "$ACTUAL_PACKAGE_NAME.queuechanged" // todo
        const val PLAY_STATE_CHANGED = "$ACTUAL_PACKAGE_NAME.playstatechanged"

        const val REPEAT_MODE_CHANGED = "$ACTUAL_PACKAGE_NAME.repeatmodechanged"
        const val SHUFFLE_MODE_CHANGED = "$ACTUAL_PACKAGE_NAME.shufflemodechanged"
        const val MEDIA_STORE_CHANGED = "$ACTUAL_PACKAGE_NAME.mediastorechanged"

        const val SAVED_POSITION_IN_TRACK = "POSITION_IN_TRACK"

        private const val THROTTLE: Long = 500

        fun log(msg: String, force: Boolean) {
            if (force || BuildConfig.DEBUG) Log.i("MusicServiceDebug", msg)
        }

        fun parsePlaylistAndPlay(intent: Intent, service: MusicService) {
            val playlist: Playlist? = intent.getParcelableExtra(
                INTENT_EXTRA_PLAYLIST
            )
            val playlistSongs = playlist?.getSongs(service)
            val shuffleMode = ShuffleMode.deserialize(
                intent.getIntExtra(INTENT_EXTRA_SHUFFLE_MODE, SHUFFLE_MODE_NONE)
            )
            if (playlistSongs.isNullOrEmpty()) {
                Toast.makeText(service, R.string.playlist_is_empty, Toast.LENGTH_LONG).show()
            } else {
                val queueManager = App.instance.queueManager
                queueManager.switchShuffleMode(shuffleMode)
                // TODO: keep the queue intact
                val queue =
                    if (shuffleMode == ShuffleMode.SHUFFLE) playlistSongs.toMutableList().apply { shuffle() } else playlistSongs
                service.openQueue(queue, 0, true)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = musicBind
    private val musicBind: IBinder = MusicBinder()
    inner class MusicBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }
}
