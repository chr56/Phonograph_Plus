package player.phonograph.service.player

import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.mechanism.lyrics.LyricsUpdater
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicService
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.service.util.makeErrorMessage
import player.phonograph.settings.Setting
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.os.PowerManager.WakeLock
import android.provider.MediaStore
import android.util.ArrayMap
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

// todo cleanup queueManager.setQueueCursor
/**
 * @author chr_56 & Abou Zeid (kabouzeid) (original author)
 */
class PlayerController(internal val service: MusicService) : Playback.PlaybackCallbacks {

    private val queueManager = App.instance.queueManager

    private val audioPlayer: AudioPlayer

    private val wakeLock: WakeLock
    private val audioFocusManager: AudioFocusManager =
        AudioFocusManager(this)

    val handler: ControllerHandler
    private var thread: HandlerThread

    private var lyricsUpdater: LyricsUpdater

    init {
        audioPlayer = AudioPlayer(service, Setting.instance.gaplessPlayback, this)

        wakeLock =
            (App.instance.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    javaClass.name
                ).apply {
                    setReferenceCounted(false)
                }

        // setup handler
        thread = HandlerThread("player_controller_handler_thread")
        thread.start()
        handler = ControllerHandler(this, thread.looper)

        lyricsUpdater = LyricsUpdater(queueManager.currentSong)
    }

    /**
     * release taken resources but not vitals
     */
    fun releaseTakenResources() {
        releaseWakeLock()
        audioFocusManager.abandonAudioFocus()
    }

    /**
     * release vital resources
     */
    fun destroy() {
        unregisterBecomingNoisyReceiver(service)
        audioPlayer.release()
        thread.quitSafely()
        handler.looper.quitSafely()
        lyricsUpdater.clear()
    }

    fun stopAndDestroy() {
        stopImp()
        destroy()
    }

    fun acquireWakeLock(milli: Long) {
        wakeLock.acquire(milli)
    }

    fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private var _playerState: PlayerState = PlayerState.PREPARING
    var playerState: PlayerState
        get() = _playerState
        private set(value) {
            val oldState = _playerState
            synchronized(_playerState) {
                _playerState = value
            }
            observers.executeForEach { onPlayerStateChanged(oldState, value) }
            _state.update { _playerState }
        }

    /**
     * prepare current and next player and set queue cursor(position)
     * @param position where to start in queue
     * @return true if it is ready
     */
    private fun prepareSongsImp(position: Int): Boolean {
        // todo: change STATE if possible
        broadcastStopLyric()
        log(
            "prepareSongsImp:Before",
            "current:${queueManager.currentSong.title} ,next:${queueManager.nextSong.title}"
        )
        queueManager.modifyPosition(position, false)
        return prepareCurrentPlayer(queueManager.currentSong).also { setCurrentSuccess ->
            if (setCurrentSuccess) prepareNextPlayer(queueManager.nextSong)
            else prepareNextPlayer(null)

            notifyNowPlayingChanged()

            log(
                "prepareSongsImp:After",
                "current:${queueManager.currentSong.title} ,next:${queueManager.nextSong.title}"
            )
        }
    }
    private fun notifyNowPlayingChanged() {
        observers.executeForEach {
            onReceivingMessage(MSG_NOW_PLAYING_CHANGED)
        }
        handler.post {
            lyricsUpdater.currentSong = queueManager.currentSong
        }
    }

    /**
     * prepare current player data source safely
     * @param song what to play now
     * @return true if success
     */
    private fun prepareCurrentPlayer(song: Song): Boolean {
        return if (song != Song.EMPTY_SONG) audioPlayer.setDataSource(
            getTrackUri(song.id).toString()
        ) else {
            false
        }
    }

    /**
     * prepare next player data source safely
     * @param song what to play now
     */
    private fun prepareNextPlayer(song: Song?) {
        audioPlayer.setNextDataSource(
            if (song != null && song != Song.EMPTY_SONG) getTrackUri(song.id).toString() else null
        )
    }

    var restored = false
    fun restoreIfNecessary() {
        if (!restored) {
            val restoredPositionInTrack =
                QueuePreferenceManager(service).currentMillisecond
            prepareSongsImp(queueManager.currentSongPosition)
            if (restoredPositionInTrack > 0) seekTo(restoredPositionInTrack.toLong())
            restored = true
        }
    }

    /**
     * Play songs from a certain position
     */
    fun playAt(position: Int) = handler.request {
        it.playAtImp(position)
    }
    private fun playAtImp(position: Int) {
        if (prepareSongsImp(position)) {
            playImp()
        } else {
            Toast.makeText(
                service,
                service.resources.getString(R.string.unplayable_file),
                Toast.LENGTH_SHORT
            ).show()
            jumpForwardImp(false)
        }
        log("playAtImp", "current: at $position song(${queueManager.currentSong.title})")
    }

    /**
     * Set position in current queue, only available if paused
     * @param position current position in playing queue
     */
    fun setPosition(position: Int) = handler.request {
        it.setPositionImp(position)
    }
    private fun setPositionImp(position: Int) {
        if (playerState == PlayerState.PAUSED) {
            queueManager.modifyPosition(position,false)
            prepareSongsImp(position)
        }
    }

    /**
     * continue play
     */
    fun play() = handler.request {
        it.playImp()
    }
    private fun playImp() {
        if (queueManager.playingQueue.isNotEmpty()) {
            if (audioFocusManager.requestAudioFocus()) {
                checkAndRegisterBecomingNoisyReceiver(service)
                if (!audioPlayer.isPlaying()) {
                    // Actual Logics Start
                    synchronized(this) {
                        if (!audioPlayer.isInitialized) {
                            playAtImp(queueManager.currentSongPosition)
                        } else {
                            audioPlayer.start()

                            playerState = PlayerState.PLAYING
                            pauseReason = NOT_PAUSED
                            acquireWakeLock(
                                queueManager.currentSong.duration - audioPlayer.position() + 1000L
                            )
                            handler.removeMessages(ControllerHandler.DUCK)
                            handler.sendEmptyMessage(ControllerHandler.UNDUCK)
                            handler.lyricsLoop() // start broadcast lyrics loop
                        }
                    }
                    // Actual Logics End
                } else {
                    log("playImp", "Already Playing!", true)
                }
            } else {
                Toast.makeText(
                    service,
                    service.resources.getString(R.string.audio_focus_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            pauseImp(true)
        }
    }

    var pauseReason: Int = NOT_PAUSED

    /**
     * Pause
     */
    fun pause() = handler.request {
        it.pauseImp()
    }
    private fun pauseImp(force: Boolean = false) {
        if (audioPlayer.isPlaying() || force) {
            audioPlayer.pause()
            broadcastStopLyric()
            playerState = PlayerState.PAUSED
            releaseTakenResources()
        }
    }

    fun togglePlayPause() = handler.request {
        it.togglePlayPauseImp()
    }
    private fun togglePlayPauseImp() {
        if (audioPlayer.isPlaying()) {
            pauseImp()
            pauseReason = PAUSE_BY_MANUAL_ACTION
        } else {
            playImp()
        }
    }

    fun isPlaying() = audioPlayer.isInitialized && audioPlayer.isPlaying()

    val positionInTrack: Int =
        if (audioPlayer.isInitialized) {
            audioPlayer.position()
        } else {
            -1
        }

    /**
     * Jump to beginning of this song
     */
    fun rewindToBeginning() = handler.request {
        it.rewindToBeginningImp()
    }
    private fun rewindToBeginningImp() {
        seekTo(0)
        service.requireRefreshMediaSessionState()
    }

    /**
     * Return to previous song
     */
    fun jumpBackward(force: Boolean) = handler.request {
        it.jumpBackwardImp(force)
    }
    private fun jumpBackwardImp(force: Boolean) {
        if (force) {
            playAtImp(queueManager.previousListPosition)
        } else {
            playAtImp(queueManager.previousSongPosition)
        }
    }

    /**
     * [rewindToBeginningImp] or [jumpBackwardImp]
     */
    fun back(force: Boolean) = handler.request {
        it.backImp(force)
    }
    private fun backImp(force: Boolean) {
        if (audioPlayer.position() > 5000) {
            rewindToBeginningImp()
        } else {
            jumpBackwardImp(force)
        }
    }

    /**
     * Skip and jump to next song
     */
    fun jumpForward(force: Boolean) = handler.request {
        it.jumpForwardImp(force)
    }
    private fun jumpForwardImp(force: Boolean) {
        if (force) {
            playAtImp(queueManager.nextListPosition)
        } else {
            if (!queueManager.isLastTrack()) {
                playAtImp(queueManager.nextSongPosition)
            } else {
                pauseImp(true)
                observers.executeForEach {
                    onReceivingMessage(MSG_NO_MORE_SONGS)
                }
            }
        }
    }

    /**
     * Move current time to [position]
     * @param position time in millisecond
     */
    fun seekTo(position: Long): Int = synchronized(audioPlayer) {
        seekToImp(position)
    }
    private fun seekToImp(position: Long): Int {
        return audioPlayer.seek(position.toInt())
    }

    fun stop() = handler.request {
        it.stopImp()
    }
    private fun stopImp() {
        audioPlayer.stop()
        broadcastStopLyric()
        playerState = PlayerState.STOPPED
        releaseTakenResources()
        observers.executeForEach {
            onReceivingMessage(MSG_PLAYER_STOPPED)
        }
    }

    /**
     * true if you want to stop player when current track is ended
     * Used by Sleep Timer
     */
    internal var quitAfterFinishCurrentSong: Boolean = false
        set(value) {
            synchronized(this) {
                field = value
            }
        }

    override fun onTrackWentToNext() {
        handler.request {
            // check sleep timer
            if (quitAfterFinishCurrentSong) {
                stopImp()
                return@request
            }
            queueManager.moveToNextSong(false)
            notifyNowPlayingChanged()
            prepareNextPlayer(queueManager.nextSong)
        }
    }

    override fun onTrackEnded() {
        // check sleep timer
        if (quitAfterFinishCurrentSong) {
            stop()
            return
        }
        if (queueManager.isQueueEnded()) {
            handler.request {
                pauseImp(true)
            }
            broadcastStopLyric()
            observers.executeForEach {
                onReceivingMessage(MSG_NO_MORE_SONGS)
            }
        } else {
            handler.request {
                prepareNextPlayer(queueManager.nextSong)
                queueManager.moveToNextSong(false)
                playAtImp(queueManager.currentSongPosition)
            }
        }
    }

    override fun onError(what: Int, extra: Int) {
        val msg = makeErrorMessage(service, what, extra)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(service, msg, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val NOT_PAUSED = 0
        const val PAUSE_BY_MANUAL_ACTION = 2
        const val PAUSE_FOR_QUEUE_ENDED = 4
        const val PAUSE_FOR_AUDIO_BECOMING_NOISY = 8
        const val PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS = 16
        const val PAUSE_ERROR = -2

        private fun getTrackUri(songId: Long): Uri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
    }

    private var becomingNoisyReceiverRegistered = false
    private fun checkAndRegisterBecomingNoisyReceiver(context: Context) {
        if (!becomingNoisyReceiverRegistered) {
            context.registerReceiver(
                becomingNoisyReceiver,
                IntentFilter(
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY
                )
            )
            becomingNoisyReceiverRegistered = true
        }
    }
    private fun unregisterBecomingNoisyReceiver(context: Context) {
        if (becomingNoisyReceiverRegistered) {
            context.unregisterReceiver(becomingNoisyReceiver)
            becomingNoisyReceiverRegistered = false
        }
    }
    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    fun saveCurrentMills() = handler.request {
        saveCurrentMillsImp()
    }
    private fun saveCurrentMillsImp() {
        QueuePreferenceManager(service).currentMillisecond = audioPlayer.position()
    }

    val audioSessionId: Int = audioPlayer.audioSessionId

    fun setVolume(vol: Float) = handler.request { playerController ->
        playerController.audioPlayer.setVolume(vol)
    }

    private val observers: MutableList<PlayerStateObserver> = ArrayList()
    fun addObserver(observer: PlayerStateObserver) = observers.add(observer)
    fun removeObserver(observer: PlayerStateObserver): Boolean = observers.remove(observer)

    class ControllerHandler(playerController: PlayerController, looper: Looper) : Handler(looper) {
        private val controllerRef: WeakReference<PlayerController> = WeakReference(playerController)

        private var requestIdCumulator = 0
        private val requestList: ArrayMap<Int, RunnableRequest> = ArrayMap(1)

        /**
         * Request running in the handler thread
         * @param request RunnableRequest: (PlayerController) -> Unit
         */
        fun request(request: RunnableRequest) {
            val requestId = requestIdCumulator++
            synchronized(requestList) {
                requestList[requestId] = request
                sendMessage(
                    Message.obtain(this, HANDLER_EXECUTE_REQUEST, requestId, 0)
                )
            }
        }

        fun command(what: Int, arg1: Int, arg2: Int) {
            sendMessage(
                Message.obtain(this, what, arg1, arg2)
            )
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                HANDLER_EXECUTE_REQUEST -> {
                    val requestId = msg.arg1
                    val controller = controllerRef.get()
                    if (controller == null) {
                        ErrorNotification.postErrorNotification(
                            "ControllerHandler: weak reference for PlayerController is missing!\n${Thread.currentThread().stackTrace}"
                        )
                        return
                    }
                    requestList[requestId]?.let { runnableRequest ->
                        runnableRequest.invoke(controller)
                        synchronized(requestList) {
                            requestList.remove(requestId)
                        }
                    }
                }
                DUCK -> {
                    controllerRef.get()?.let {
                        if (Setting.instance.audioDucking) {
                            currentDuckVolume -= .05f
                            if (currentDuckVolume > .2f) {
                                sendEmptyMessageDelayed(DUCK, 10)
                            } else {
                                currentDuckVolume = .2f
                            }
                        } else {
                            currentDuckVolume = 1f
                        }
                        it.audioPlayer.setVolume(currentDuckVolume)
                    }
                }
                UNDUCK -> {
                    controllerRef.get()?.let {
                        if (Setting.instance.audioDucking) {
                            currentDuckVolume += .03f
                            if (currentDuckVolume < 1f) {
                                sendEmptyMessageDelayed(UNDUCK, 10)
                            } else {
                                currentDuckVolume = 1f
                            }
                        } else {
                            currentDuckVolume = 1f
                        }
                        it.audioPlayer.setVolume(currentDuckVolume)
                    }
                }
                RE_PREPARE_NEXT_PLAYER -> controllerRef.get()?.let {
                    synchronized(it.audioPlayer) {
                        it.prepareNextPlayer(it.queueManager.nextSong)
                    }
                }
                CLEAN_NEXT_PLAYER -> controllerRef.get()?.let {
                    synchronized(it.audioPlayer) {
                        it.prepareNextPlayer(null)
                    }
                }
            }
        }

        private var currentDuckVolume = 1.0f

        /**
         * @return true if continue
         */
        private fun broadcastLyrics(): Boolean {
            val controller = controllerRef.get() ?: return false
            if (controller.playerState != PlayerState.PLAYING ||
                !Setting.instance.broadcastSynchronizedLyrics
            ) {
                controller.broadcastStopLyric()
                return false
            }

            controller.lyricsUpdater.broadcast(controller.getSongProgressMillis())
            return true
        }

        /**
         * start loop to broadcast lyrics
         */
        fun lyricsLoop() {
            postDelayed({
                if (broadcastLyrics()) lyricsLoop()
            }, 750)
        }

        companion object {
            // MSG WHAT
            private const val HANDLER_EXECUTE_REQUEST = 10

            const val DUCK = 20
            const val UNDUCK = 30

            const val RE_PREPARE_NEXT_PLAYER = 40
            const val CLEAN_NEXT_PLAYER = 41
        }
    }

    fun getSongProgressMillis(): Int = audioPlayer.position()
    fun getSongDurationMillis(): Int = audioPlayer.duration()

    fun switchGaplessPlayback(gaplessPlayback: Boolean) { audioPlayer.gaplessPlayback = gaplessPlayback }

    private fun broadcastStopLyric() = StatusBarLyric.stopLyric()
    fun replaceLyrics(lyrics: LrcLyrics?) {
        if (lyrics != null) {
            lyricsUpdater.forceReplaceLyrics(lyrics)
        } else {
            lyricsUpdater.clear()
        }
    }

    fun log(where: String, msg: String, force: Boolean = false) {
        if (DEBUG || force) Log.i("PlayerController", "※$msg @$where")
    }

    /*  debug */
    fun dumpPlayingQueue(where: String) {
        val msg = "${queueManager.playingQueue.foldIndexed("PlayingQueue:") { index, acc, s ->
            "$acc\n ${if (index == queueManager.currentSongPosition) "#" else " "}$index:${s.title}"
        }}\n--CurrentPosition: ${queueManager.currentSongPosition}--"
        log(where, msg)
    }
}

/**
 *  the exposed internal state as StateFlow for collect in ui.
 */
val PlayerController.Companion.currentState: StateFlow<PlayerState>
    get() = _state.asStateFlow()

@Suppress("ObjectPropertyName")
private val _state by lazy { MutableStateFlow(PlayerState.PREPARING) }
