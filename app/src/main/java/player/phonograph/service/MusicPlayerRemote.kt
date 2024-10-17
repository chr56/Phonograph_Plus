/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.service.MusicService.MusicBinder
import player.phonograph.service.player.PlayerState
import player.phonograph.service.player.PlayerStateObserver
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.debug
import player.phonograph.util.reportError
import player.phonograph.util.warning
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.withStarted
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.WeakHashMap

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object MusicPlayerRemote {
    var musicService: MusicService? = null
        private set
    private val mConnectionMap = WeakHashMap<Context, MusicServiceConnection>()

    val queueManager: QueueManager by GlobalContext.get().inject()

    suspend fun bindToService(
        activity: ComponentActivity,
        callback: MusicServiceConnection?,
    ): ServiceToken? {
        @Suppress("DEPRECATION")
        val contextWrapper = ContextWrapper(
            activity.parent ?: activity // try to use parent activity
        )
        return activity.lifecycle.withStarted {
            try {
                // start service
                contextWrapper.startService(Intent(contextWrapper, MusicService::class.java))
            } catch (e: Exception) {
                reportError(e, TAG, "Failed to start service")
            }

            val serviceConnection = MusicServiceConnectionImpl(callback)

            // bind service
            if (
                contextWrapper.bindService(
                    Intent().setClass(contextWrapper, MusicService::class.java),
                    serviceConnection,
                    BIND_AUTO_CREATE
                )
            ) {
                debug { Log.v(TAG, "${activity.javaClass.simpleName} is successfully bind to service!") }
                mConnectionMap[contextWrapper] = serviceConnection
                ServiceToken(contextWrapper)
            } else {
                warning(TAG, "Failed to start MusicService")
                null
            }
        }

    }

    fun unbindFromService(token: ServiceToken?) {
        if (token == null) return

        val mContextWrapper = token.mWrappedContext
        val mBinder = mConnectionMap.remove(mContextWrapper) ?: return

        mContextWrapper.unbindService(mBinder)

        if (mConnectionMap.isEmpty()) {
            musicService = null
        }
    }

    fun unbindAllFromService() {
        synchronized(mConnectionMap) {
            for ((context, connection) in mConnectionMap) {
                context.unbindService(connection)
                connection.onServiceDetached()
            }
            mConnectionMap.clear()
            musicService = null
        }
    }

    class MusicServiceConnectionImpl(private val mCallback: MusicServiceConnection?) : MusicServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            musicService = (service as MusicBinder).service
            mCallback?.onServiceConnected(className, service)
            if (resumeInstantlyIfReady) {
                service.service.play()
                Log.v(TAG, "Resume eagerly due to setting!")
                resumeInstantlyIfReady = false
            }
            musicService?.addPlayerStateObserver(playerStateObserver)
        }

        override fun onServiceDetached() {
            mCallback?.onServiceDetached()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            musicService?.removePlayerStateObserver(playerStateObserver)
            mCallback?.onServiceDisconnected(className)
            musicService = null
        }

        @RequiresApi(VERSION_CODES.O)
        override fun onBindingDied(name: ComponentName?) {
            mCallback?.onBindingDied(name)
        }

        @RequiresApi(VERSION_CODES.P)
        override fun onNullBinding(name: ComponentName?) {
            mCallback?.onNullBinding(name)
        }
    }

    class ServiceToken(var mWrappedContext: ContextWrapper)

    /**
     * Async
     */
    fun playSongAt(position: Int) {
        musicService?.playSongAt(position)
    }

    val isPlaying: Boolean get() = musicService != null && musicService!!.isPlaying

    fun pauseSong() {
        musicService?.pause()
    }

    fun resumePlaying() {
        musicService?.play()
    }


    private val _currentState: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.PREPARING)
    val currentState: StateFlow<PlayerState> = _currentState.asStateFlow()

    private val playerStateObserver: PlayerStateObserver = object : PlayerStateObserver {
        override fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {
            _currentState.update { newState }
        }

        override fun onReceivingMessage(msg: Int) {}
    }

    private var resumeInstantlyIfReady: Boolean = false

    fun requireResumeInstantlyIfReady() {
        val service = musicService
        if (service == null) {
            resumeInstantlyIfReady = true
        } else {
            service.playSongAt(position)
        }
    }

    fun cancelResumeInstantlyIfReady() {
        resumeInstantlyIfReady = false
    }

    /**
     * Async
     */
    fun playNextSong() {
        musicService?.playNextSong(true)
    }

    /**
     * Async
     */
    fun playPreviousSong() {
        musicService?.playPreviousSong(true)
    }

    /**
     * Async
     */
    fun back() {
        musicService?.back(true)
    }

    /**
     * Play a queue (asynchronous)
     * @param queue new queue
     * @param startPlaying true if to play now, false if to pause
     * @param shuffleMode new preferred shuffle mode, null if no intend to change
     * @param startPosition * if Shuffle Mode on, selected position as first song; if off, position in queue when starting playing
     * @return request success or not
     */
    fun playQueue(
        queue: List<Song>,
        startPosition: Int,
        startPlaying: Boolean,
        shuffleMode: ShuffleMode?,
    ): Boolean {
        if (queue.isEmpty() || startPosition !in queue.indices) {
            warning(
                TAG,
                "Queue(size:${queue.size}) submitted is empty or start position ($startPosition) is out ranged"
            )
            return false
        }
        operatorHandler.post {
            // check whether queue already sits there
            if (queueManager.playingQueue === queue) {
                if (startPlaying) {
                    playSongAt(startPosition)
                } else {
                    queueManager.modifyPosition(startPosition, false)
                }
                return@post
            }
            // swap queue
            shuffleMode?.let { queueManager.modifyShuffleMode(shuffleMode, false) }
            queueManager.swapQueue(queue, startPosition, false)
            if (startPlaying) musicService?.playSongAt(queueManager.currentSongPosition)
            else musicService?.pause()
        }
        return true
    }

    val currentSong: Song get() = queueManager.currentSong
    val previousSong: Song get() = queueManager.previousSong
    val nextSong: Song get() = queueManager.nextSong

    /**
     * Async
     */
    var position: Int
        get() = queueManager.currentSongPosition
        set(position) {
            queueManager.modifyPosition(position, false)
        }

    val playingQueue: List<Song>
        get() = queueManager.playingQueue
    val songProgressMillis: Int
        get() = musicService?.songProgressMillis ?: -1
    val songDurationMillis: Int
        get() = musicService?.songDurationMillis ?: -1

    fun getQueueDurationMillis(position: Int): Long = queueManager.getRestSongsDuration(position)

    fun seekTo(millis: Int): Int {
        return musicService?.seek(millis) ?: -1
    }

    val repeatMode: RepeatMode
        get() = queueManager.repeatMode
    val shuffleMode: ShuffleMode
        get() = queueManager.shuffleMode

    fun cycleRepeatMode(): Boolean =
        runCatching {
            queueManager.cycleRepeatMode()
        }.isSuccess

    fun toggleShuffleMode(): Boolean =
        runCatching {
            queueManager.toggleShuffle()
        }.isSuccess

    fun setShuffleMode(shuffleMode: ShuffleMode): Boolean =
        runCatching {
            queueManager.modifyShuffleMode(shuffleMode)
        }.isSuccess

    fun playNow(song: Song): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                playQueue(listOf(song), 0, false, null)
            } else {
                queueManager.addSong(song, position)
                it.playSongAt(position)
            }
            Toast.makeText(
                musicService,
                it.resources.getString(R.string.added_title_to_playing_queue),
                LENGTH_SHORT
            ).show()
        }
    }

    fun playNow(songs: List<Song>): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                playQueue(songs, 0, false, null)
                it.play()
            } else {
                queueManager.addSongs(songs, position)
                it.playSongAt(position)
            }
            Toast.makeText(
                musicService,
                if (songs.size == 1) it.resources.getString(R.string.added_title_to_playing_queue)
                else it.resources.getString(R.string.added_x_titles_to_playing_queue, songs.size),
                LENGTH_SHORT
            ).show()
        }
    }

    fun playNext(song: Song): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                playQueue(listOf(song), 0, false, null)
            } else {
                queueManager.addSong(song, position + 1)
            }
            Toast.makeText(
                musicService,
                it.resources.getString(R.string.added_title_to_playing_queue),
                LENGTH_SHORT
            ).show()
        }
    }

    @JvmStatic
    fun playNext(songs: List<Song>): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                playQueue(songs, 0, false, null)
            } else {
                queueManager.addSongs(songs, position + 1)
            }

            Toast.makeText(
                musicService,
                if (songs.size == 1) it.resources.getString(R.string.added_title_to_playing_queue)
                else it.resources.getString(R.string.added_x_titles_to_playing_queue, songs.size),
                LENGTH_SHORT
            ).show()
        }
    }

    fun enqueue(song: Song): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                playQueue(listOf(song), 0, false, null)
            } else {
                queueManager.addSong(song)
            }

            Toast.makeText(
                musicService,
                it.resources.getString(R.string.added_title_to_playing_queue),
                LENGTH_SHORT
            ).show()
        }
    }

    @JvmStatic
    fun enqueue(songs: List<Song>): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                playQueue(songs, 0, false, null)
            } else {
                queueManager.addSongs(songs)
            }

            Toast.makeText(
                musicService,
                if (songs.size == 1) it.resources.getString(R.string.added_title_to_playing_queue)
                else it.resources.getString(R.string.added_x_titles_to_playing_queue, songs.size),
                LENGTH_SHORT
            ).show()
        }
    }

    fun removeFromQueue(song: Song): Boolean {
        return musicService.tryExecute {
            queueManager.removeSong(song)
        }
    }

    fun removeFromQueue(position: Int): Boolean {
        return musicService.tryExecute {
            if (position in 0..playingQueue.size) queueManager.removeSongAt(position)
        }
    }

    fun moveSong(from: Int, to: Int): Boolean {
        return musicService.tryExecute {
            if (from in 0..playingQueue.size && to in 0..playingQueue.size) {
                queueManager.moveSong(from, to)
            }
        }
    }

    fun clearQueue(): Boolean =
        runCatching {
            queueManager.clearQueue()
        }.isSuccess

    val audioSessionId: Int get() = musicService?.audioSessionId ?: -1

    val isServiceConnected: Boolean get() = musicService != null

    const val TAG = "MusicPlayerRemote"

    val operatorHandler: Handler by lazy {
        HandlerThread("worker").let {
            it.start()
            Handler(it.looper)
        }
    }

    private inline fun MusicService?.tryExecute(p: (obj: MusicService) -> Unit): Boolean {
        return if (this != null) {
            p(this)
            true
        } else {
            false
        }
    }
}
