package player.phonograph.service

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.audiofx.AudioEffect
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import player.phonograph.App
import player.phonograph.App.Companion.ACTUAL_PACKAGE_NAME
import player.phonograph.BuildConfig
import player.phonograph.glide.BlurTransformation
import player.phonograph.glide.SongGlideRequest
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LrcLyrics
import player.phonograph.notification.PlayingNotification
import player.phonograph.notification.PlayingNotificationImpl
import player.phonograph.notification.PlayingNotificationImpl24
import player.phonograph.provider.HistoryStore
import player.phonograph.provider.SongPlayCountStore
import player.phonograph.service.MusicServiceKt.Companion.copy
import player.phonograph.service.MusicServiceKt.Companion.parsePlaylistAndPlay
import player.phonograph.service.player.MSG_NOW_PLAYING_CHANGED
import player.phonograph.service.player.PlayerController
import player.phonograph.service.player.PlayerState
import player.phonograph.service.player.PlayerStateObserver
import player.phonograph.service.queue.QueueChangeObserver
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.service.util.SongPlayCountHelper
import player.phonograph.settings.Setting
import player.phonograph.util.Util.getScreenSize

/**
 * @author Karim Abou Zeid (kabouzeid), Andrew Neal
 */
class MusicService : Service(), OnSharedPreferenceChangeListener {

    private val songPlayCountHelper = SongPlayCountHelper()

    var pendingQuit = false // todo sleeptimer

    private var musicServiceKt: MusicServiceKt? = null
    private var queueManager: QueueManager? = null
    private var queueChangeObserver: QueueChangeObserver? = null
    private var controller: PlayerController? = null
    private var playerStateObserver: PlayerStateObserver? = null
    private var playingNotification: PlayingNotification? = null
    var mediaSession: MediaSessionCompat? = null
        private set
    private var throttledSeekHandler: ThrottledSeekHandler? = null
    private var uiThreadHandler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        queueManager = App.instance.queueManager
        controller = PlayerController(this)
        setupMediaSession()
        uiThreadHandler = Handler(Looper.getMainLooper())
        musicServiceKt = MusicServiceKt(this)
        registerReceiver(musicServiceKt!!.widgetIntentReceiver, IntentFilter(APP_WIDGET_UPDATE))
        initNotification()

        // todo use other handler
        musicServiceKt!!.setUpMediaStoreObserver(
            this,
            controller!!.handler,
            this@MusicService::handleAndSendChangeInternal
        )
        throttledSeekHandler = ThrottledSeekHandler(controller!!.handler)
        Setting.instance().registerOnSharedPreferenceChangedListener(this)

        // notify manually for first setting up queueManager
        sendChangeInternal(META_CHANGED)
        sendChangeInternal(QUEUE_CHANGED)
        controller!!.restoreIfNecessary()
        mediaSession!!.isActive = true
        queueChangeObserver = initQueueChangeObserver()
        queueManager!!.addObserver(queueChangeObserver!!)
        playerStateObserver = initPlayerStateObserver()
        controller!!.addObserver(playerStateObserver!!)
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_CREATED"))
    }

    private fun initQueueChangeObserver(): QueueChangeObserver {
        return object : QueueChangeObserver {
            override fun onStateSaved() {}
            override fun onStateRestored() {}
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
                controller!!.handler.removeMessages(
                    PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                )
                controller!!.handler.sendEmptyMessage(
                    PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                )
                notifyChange(REPEAT_MODE_CHANGED)
            }
        }
    }

    private fun initPlayerStateObserver(): PlayerStateObserver {
        return object : PlayerStateObserver {
            override fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {
                notifyChange(PLAY_STATE_CHANGED)
            }

            override fun onReceivingMessage(msg: Int) {
                when (msg) {
                    MSG_NOW_PLAYING_CHANGED -> notifyChange(META_CHANGED)
                }
            }
        }
    }

    private fun setupMediaSession() {
        val mediaButtonReceiverComponentName = ComponentName(
            applicationContext,
            MediaButtonIntentReceiver::class.java
        )
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.component = mediaButtonReceiverComponentName
        val mediaButtonReceiverPendingIntent =
            PendingIntent.getBroadcast(
                applicationContext,
                0,
                mediaButtonIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        mediaSession =
            MediaSessionCompat(
                this,
                BuildConfig.APPLICATION_ID,
                mediaButtonReceiverComponentName,
                mediaButtonReceiverPendingIntent
            )
        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
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
        })

        // fixme remove deprecation
        @Suppress("DEPRECATION")
        mediaSession!!.setFlags(
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
        )
        mediaSession!!.setMediaButtonReceiver(mediaButtonReceiverPendingIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action != null) {
                controller!!.restoreIfNecessary()
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

    var isIdle = false
        private set
    override fun onDestroy() {
        isIdle = true
        mediaSession!!.isActive = false
        playingNotification!!.stop()
        closeAudioEffectSession()
        mediaSession!!.release()
        unregisterReceiver(musicServiceKt!!.widgetIntentReceiver)
        musicServiceKt!!.unregisterMediaStoreObserver(this)
        Setting.instance.unregisterOnSharedPreferenceChangedListener(this)
        controller!!.stopAndDestroy()
        controller!!.removeObserver(playerStateObserver!!)
        queueManager!!.removeObserver(queueChangeObserver!!)
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_DESTROYED"))
    }

    val isPlaying: Boolean
        get() = controller!!.isPlaying()

    override fun onBind(intent: Intent): IBinder = musicBind

    private fun savePositionInTrack() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(
            SAVED_POSITION_IN_TRACK,
            songProgressMillis
        ).apply()
    }

    private fun saveState() {
        queueManager!!.postMessage(QueueManager.MSG_SAVE_QUEUE)
        queueManager!!.postMessage(QueueManager.MSG_SAVE_CURSOR)
        savePositionInTrack()
    }

    fun playNextSong(force: Boolean) {
        controller!!.jumpForward(force)
    }

    // todo
    private fun closeAudioEffectSession() {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, controller!!.audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(audioEffectsIntent)
    }

    private fun initNotification() {
        playingNotification =
            if (!Setting.instance.classicNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PlayingNotificationImpl24(this)
            } else {
                PlayingNotificationImpl(this)
            }
    }

    private fun updateNotification() {
        val song = queueManager!!.currentSong
        if (playingNotification != null && song.id != -1L) {
            playingNotification!!.metaData = PlayingNotification.SongMetaData(song)
        }
    }

    private fun updateMediaSessionPlaybackState() {
        mediaSession!!.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    songProgressMillis.toLong(),
                    1f
                )
                .build()
        )
    }

    private fun updateMediaSessionMetaData() {
        val song = queueManager!!.currentSong
        if (song.id == -1L) {
            mediaSession!!.setMetadata(null)
            return
        }
        val metaData = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                (queueManager!!.currentSongPosition + 1).toLong()
            )
            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.year.toLong())
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,
                queueManager!!.playingQueue.size.toLong()
            )
        if (Setting.instance.albumArtOnLockscreen) {
            val screenSize = getScreenSize(this@MusicService)
            val request = SongGlideRequest.Builder.from(Glide.with(this@MusicService), song)
                .checkIgnoreMediaStore(this@MusicService)
                .asBitmap().build()
            if (Setting.instance.blurredAlbumArt) {
                request.transform(BlurTransformation.Builder(this@MusicService).build())
            }
            runOnUiThread {
                request.into(
                    object : CustomTarget<Bitmap>(screenSize.x, screenSize.y) {
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            mediaSession!!.setMetadata(metaData.build())
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            metaData.putBitmap(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                                copy(resource)
                            )
                            mediaSession!!.setMetadata(metaData.build())
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            mediaSession!!.setMetadata(metaData.build()) // todo check leakage
                        }
                    }
                )
            }
        } else {
            mediaSession!!.setMetadata(metaData.build())
        }
    }

    fun runOnUiThread(runnable: Runnable?) {
        uiThreadHandler!!.post(runnable!!)
    }

    fun openQueue(playingQueue: List<Song>?, startPosition: Int, startPlaying: Boolean) {
        if (playingQueue != null && playingQueue.isNotEmpty() && startPosition >= 0 && startPosition < playingQueue.size) {
            queueManager!!.swapQueue(playingQueue, startPosition, false)
            if (startPlaying) {
                playSongAt(queueManager!!.currentSongPosition)
            }
            notifyChange(QUEUE_CHANGED)
        }
    }

    fun playSongAt(position: Int) {
        controller!!.playAt(position)
    }

    fun pause() {
        controller!!.pause()
    }

    fun play() {
        controller!!.play()
    }

    fun playPreviousSong(force: Boolean) {
        controller!!.jumpBackward(force)
    }

    fun back(force: Boolean) {
        if (songProgressMillis > 5000) {
            seek(0)
        } else {
            playPreviousSong(force)
        }
    }

    val songProgressMillis: Int
        get() = controller!!.getSongProgressMillis()
    val songDurationMillis: Int
        get() = controller!!.getSongDurationMillis()

    // todo check
    fun seek(millis: Int): Int {
        synchronized(this) {
            return try {
                val newPosition = controller!!.seekTo(millis.toLong())
                throttledSeekHandler!!.notifySeek()
                newPosition
            } catch (e: Exception) {
                -1
            }
        }
    }

    private fun notifyChange(what: String) {
        handleAndSendChangeInternal(what)
        sendPublicIntent(what)
    }

    private fun handleAndSendChangeInternal(what: String) {
        handleChangeInternal(what)
        sendChangeInternal(what)
    }

    // to let other apps know whats playing. i.E. last.fm (scrobbling) or musixmatch
    private fun sendPublicIntent(what: String) {
        MusicServiceKt.sendPublicIntent(this, what)
    }

    private fun sendChangeInternal(what: String) {
        sendBroadcast(Intent(what))
        musicServiceKt!!.appWidgetBig.notifyChange(this, what)
        musicServiceKt!!.appWidgetClassic.notifyChange(this, what)
        musicServiceKt!!.appWidgetSmall.notifyChange(this, what)
        musicServiceKt!!.appWidgetCard.notifyChange(this, what)
    }

    private fun handleChangeInternal(what: String) {
        when (what) {
            PLAY_STATE_CHANGED -> {
                updateNotification()
                updateMediaSessionPlaybackState()
                val isPlaying = isPlaying
                if (!isPlaying && songProgressMillis > 0) {
                    savePositionInTrack()
                }
                songPlayCountHelper.notifyPlayStateChanged(isPlaying)
            }
            META_CHANGED -> {
                updateNotification()
                updateMediaSessionMetaData()
                queueManager!!.postMessage(QueueManager.MSG_SAVE_CURSOR)
                savePositionInTrack()
                val currentSong = queueManager!!.currentSong
                HistoryStore.getInstance(this).addSongId(currentSong.id)
                if (songPlayCountHelper.shouldBumpPlayCount()) {
                    SongPlayCountStore.getInstance(this).bumpPlayCount(songPlayCountHelper.song.id)
                }
                songPlayCountHelper.notifySongChanged(currentSong)
            }
            QUEUE_CHANGED -> {
                updateMediaSessionMetaData() // because playing queue size might have changed
                saveState()
                if (queueManager!!.playingQueue.isNotEmpty()) {
                    controller!!.handler.removeMessages(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                    controller!!.handler.sendEmptyMessage(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                } else {
                    controller!!.stop()
                    playingNotification!!.stop()
                }
            }
        }
    }

    val audioSessionId: Int
        get() = controller!!.audioSessionId

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            Setting.GAPLESS_PLAYBACK -> // todo
                if (sharedPreferences.getBoolean(key, false)) {
                    controller!!.handler.removeMessages(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                    controller!!.handler.sendEmptyMessage(
                        PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER
                    )
                } else {
                    controller!!.handler.removeMessages(
                        PlayerController.ControllerHandler.CLEAN_NEXT_PLAYER
                    )
                    controller!!.handler.sendEmptyMessage(
                        PlayerController.ControllerHandler.CLEAN_NEXT_PLAYER
                    )
                }
            Setting.ALBUM_ART_ON_LOCKSCREEN, Setting.BLURRED_ALBUM_ART -> updateMediaSessionMetaData()
            Setting.COLORED_NOTIFICATION -> updateNotification()
            Setting.CLASSIC_NOTIFICATION -> {
                initNotification()
                updateNotification()
            }
        }
    }

    fun replaceLyrics(lyrics: LrcLyrics?) {
        controller!!.replaceLyrics(lyrics)
    }

    private inner class ThrottledSeekHandler(private val mHandler: Handler) : Runnable {
        fun notifySeek() {
            updateMediaSessionMetaData()
            updateMediaSessionPlaybackState()
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, THROTTLE)
        }

        override fun run() {
            savePositionInTrack()
            sendPublicIntent(PLAY_STATE_CHANGED) // for musixmatch synced lyrics
        }
    }

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
    }

    private val musicBind: IBinder = MusicBinder()
    inner class MusicBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }
}
