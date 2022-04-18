/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service

import android.app.Activity
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.MediaStore.Audio.AudioColumns.DATA
import android.provider.MediaStore.Audio.AudioColumns._ID
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import player.phonograph.R
import player.phonograph.loader.SongLoader.getSongs
import player.phonograph.loader.SongLoader.makeSongCursor
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicService.MusicBinder
import player.phonograph.settings.Setting.Companion.instance
import java.io.File
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object MusicPlayerRemote {
    var musicService: MusicService? = null
        private set
    private val mConnectionMap = WeakHashMap<Context, ServiceBinder>()

    fun bindToService(
        context: Context,
        callback: ServiceConnection?
    ): ServiceToken? {
        val realActivity = (context as Activity).parent ?: context
        val contextWrapper = ContextWrapper(realActivity)

        contextWrapper.startService(Intent(contextWrapper, MusicService::class.java))

        val binder = ServiceBinder(callback)

        if (
            contextWrapper.bindService(Intent().setClass(contextWrapper, MusicService::class.java), binder, BIND_AUTO_CREATE)
        ) {
            mConnectionMap[contextWrapper] = binder
            return ServiceToken(contextWrapper)
        }

        return null
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

    class ServiceBinder(private val mCallback: ServiceConnection?) : ServiceConnection {

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
     * Async
     */
    @JvmStatic
    fun openQueue(queue: List<Song>, startPosition: Int, startPlaying: Boolean) {
        if (!tryToHandleOpenPlayingQueue(queue, startPosition, startPlaying) && musicService != null) {
            musicService!!.openQueue(queue, startPosition, startPlaying)
            if (!instance().rememberShuffle) {
                setShuffleMode(MusicService.SHUFFLE_MODE_NONE)
            }
        }
    }

    /**
     * Async
     */
    @JvmStatic
    fun openAndShuffleQueue(queue: List<Song>, startPlaying: Boolean) {
        var startPosition = 0
        if (queue.isNotEmpty()) {
            startPosition = Random().nextInt(queue.size)
        }
        if (!tryToHandleOpenPlayingQueue(queue, startPosition, startPlaying) && musicService != null) {
            openQueue(queue, startPosition, startPlaying)
            setShuffleMode(MusicService.SHUFFLE_MODE_SHUFFLE)
        }
    }

    private fun tryToHandleOpenPlayingQueue(queue: List<Song>, startPosition: Int, startPlaying: Boolean): Boolean {
        if (playingQueue === queue) {
            if (startPlaying) {
                playSongAt(startPosition)
            } else {
                position = startPosition
            }
            return true
        }
        return false
    }

    val currentSong: Song get() = musicService?.currentSong ?: Song.EMPTY_SONG

    /**
     * Async
     */
    var position: Int
        get() = musicService?.position ?: -1
        set(position) {
            musicService?.position = position
        }

    val playingQueue: List<Song>
        get() = musicService?.playingQueue ?: ArrayList()
    val songProgressMillis: Int
        get() = musicService?.songProgressMillis ?: -1
    val songDurationMillis: Int
        get() = musicService?.songDurationMillis ?: -1

    fun getQueueDurationMillis(position: Int): Long {
        return musicService?.getQueueDurationMillis(position) ?: -1
    }

    fun seekTo(millis: Int): Int {
        return musicService?.seek(millis) ?: -1
    }

    val repeatMode: Int
        get() = musicService?.repeatMode ?: MusicService.REPEAT_MODE_NONE
    val shuffleMode: Int
        get() = musicService?.shuffleMode ?: MusicService.SHUFFLE_MODE_NONE

    fun cycleRepeatMode(): Boolean {
        return musicService.tryExecute {
            it.cycleRepeatMode()
        }
    }

    fun toggleShuffleMode(): Boolean {
        return musicService.tryExecute {
            it.toggleShuffle()
        }
    }

    fun setShuffleMode(shuffleMode: Int): Boolean {
        return musicService.tryExecute {
            it.shuffleMode = shuffleMode
        }
    }

    fun playNext(song: Song): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                openQueue(listOf(song), 0, false)
            } else {
                it.addSong(position + 1, song)
            }
            Toast.makeText(musicService, it.resources.getString(R.string.added_title_to_playing_queue), LENGTH_SHORT)
                .show()
        }
    }

    @JvmStatic
    fun playNext(songs: List<Song>): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                openQueue(songs, 0, false)
            } else {
                it.addSongs(position + 1, songs)
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
                openQueue(listOf(song), 0, false)
            } else {
                it.addSong(song)
            }

            Toast.makeText(musicService, it.resources.getString(R.string.added_title_to_playing_queue), LENGTH_SHORT)
                .show()
        }
    }

    @JvmStatic
    fun enqueue(songs: List<Song>): Boolean {
        return musicService.tryExecute {
            if (playingQueue.isEmpty()) {
                openQueue(songs, 0, false)
            } else {
                it.addSongs(songs)
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
            it.removeSong(song)
        }
    }

    fun removeFromQueue(position: Int): Boolean {
        return musicService.tryExecute {
            if (position in 0..playingQueue.size) it.removeSong(position)
        }
    }

    fun moveSong(from: Int, to: Int): Boolean {
        return musicService.tryExecute {
            if (from in 0..playingQueue.size && to in 0..playingQueue.size) it.moveSong(from, to)
        }
    }

    fun clearQueue(): Boolean {
        return musicService.tryExecute {
            it.clearQueue()
        }
    }

    val audioSessionId: Int get() = musicService?.audioSessionId ?: -1

    @Suppress("DEPRECATION")
    fun playFromUri(uri: Uri) {
        musicService.tryExecute {

            var songs: List<Song>? = null

            if (uri.scheme != null && uri.authority != null) {
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    val songId =
                        when (uri.authority) {
                            "com.android.providers.media.documents" -> {
                                getSongIdFromMediaProvider(uri)
                            }
                            "media" -> {
                                uri.lastPathSegment
                            }
                            else -> null
                        }
                    if (songId != null) {
                        songs = getSongs(makeSongCursor(it, "$_ID=?", arrayOf(songId)))
                    }
                }
            }

            if (songs == null) {

                val file: File? =
                    if (uri.authority != null && uri.authority == "com.android.externalstorage.documents") {
                        File(Environment.getExternalStorageDirectory(), uri.path!!.split(Regex("^.*:.*$"), 2)[1])
                    } else {
                        val path = getFilePathFromUri(it, uri)
                        when {
                            path != null -> {
                                File(path)
                            }
                            uri.path != null -> {
                                File(uri.path!!)
                            }
                            else -> null
                        }
                    }

                if (file != null) {
                    songs = getSongs(makeSongCursor(it, "$DATA=?", arrayOf(file.absolutePath)))
                }
            }
            if (songs != null && songs.isNotEmpty()) {
                openQueue(songs, 0, true)
            } else {
                // TODO the file is not listed in the media store
            }
        }
    }

    private fun getFilePathFromUri(context: Context, uri: Uri): String? {

        val column = "_data"
        val projection = arrayOf(column)

        val cursor: Cursor? =
            context.contentResolver.query(
                uri, projection, null, null, null
            )

        runCatching {
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(column)
                    return it.getString(columnIndex)
                }
            }
        }.also {
            if (it.isFailure && it.exceptionOrNull() != null) {
                val errMsg = it.exceptionOrNull()?.stackTraceToString().orEmpty()
                ErrorNotification.init()
                ErrorNotification.postErrorNotification(it.exceptionOrNull()!!, errMsg)
                Log.e(TAG, errMsg)
            }
        }

        return null
    }

    private fun getSongIdFromMediaProvider(uri: Uri): String = DocumentsContract.getDocumentId(uri).split(":")[1]

    val isServiceConnected: Boolean get() = musicService != null

    const val TAG = "MusicPlayerRemote"

    private fun MusicService?.tryExecute(p: (obj: MusicService) -> Unit): Boolean {
        return if (this != null) {
            p(this)
            true
        } else false
    }
}
