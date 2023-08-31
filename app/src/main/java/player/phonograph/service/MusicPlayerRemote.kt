/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicService.MusicBinder
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import java.util.WeakHashMap

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object MusicPlayerRemote {
    var musicService: MusicService? = null
        private set
    private val mConnectionMap = WeakHashMap<Context, MusicServiceConnection>()

    val queueManager: QueueManager by GlobalContext.get().inject()

    fun bindToService(
        activity: Activity,
        callback: ServiceConnection?,
    ): ServiceToken? {
        val contextWrapper = ContextWrapper(
            activity.parent ?: activity // try to use parent activity
        )
        // start service
        contextWrapper.startService(Intent(contextWrapper, MusicService::class.java))

        val serviceConnection = MusicServiceConnection(callback)

        // bind service
        return if (
            contextWrapper.bindService(
                Intent().setClass(contextWrapper, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )
        ) {
            mConnectionMap[contextWrapper] = serviceConnection
            ServiceToken(contextWrapper)
        } else null
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

    class MusicServiceConnection(private val mCallback: ServiceConnection?) : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            musicService = (service as MusicBinder).service
            mCallback?.onServiceConnected(className, service)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            musicService = null
            mCallback?.onServiceDisconnected(className)
        }
    }

    class ServiceToken(var mWrappedContext: ContextWrapper)

    /**
     * Async
     */
    fun playSongAt(position: Int) {
        musicService?.playSongAt(position)
    }

    fun pauseSong() {
        musicService?.pause()
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

    val isPlaying: Boolean get() = musicService != null && musicService!!.isPlaying

    fun resumePlaying() {
        musicService?.play()
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
            ErrorNotification.postErrorNotification(
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
            )
                .show()
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
