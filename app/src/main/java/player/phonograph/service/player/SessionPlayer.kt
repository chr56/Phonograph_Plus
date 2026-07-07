/*
 *  Copyright (c) 2026 chr_56
 */

package player.phonograph.service.player

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import player.phonograph.model.Song
import player.phonograph.model.service.PlayerState
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.browser.toMediaItem
import player.phonograph.repo.browser.toMediaMetadata
import player.phonograph.service.MusicService
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import android.os.Handler
import android.os.Looper

/** Media3 view of the existing audio engine. All state notifications use the main looper. */
@OptIn(UnstableApi::class)
internal class SessionPlayer(private val service: MusicService) : SimpleBasePlayer(Looper.getMainLooper()) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val controller get() = service.controller
    private val queueManager get() = service.queueManager
    private var artworkSong: Song? = null
    private var artwork: ByteArray? = null
    @Volatile private var released = false
    private val pendingCommands = mutableSetOf<SettableFuture<Void>>()
    private var cachedQueue: List<Song> = emptyList()
    private var playlist: List<MediaItemData> = emptyList()

    fun refresh() {
        if (!released) invalidateState()
    }

    fun setArtwork(song: Song?, bytes: ByteArray?) {
        artworkSong = song
        artwork = bytes
        cachedQueue = emptyList()
        refresh()
    }

    override fun getState(): State {
        val queue = queueManager.playingQueue
        if (queue !== cachedQueue) {
            playlist = queue.mapIndexed { index, song ->
                val metadata = song.toMediaMetadata().buildUpon()
                    .setTrackNumber(index + 1)
                    .setTotalTrackCount(queue.size)
                    .apply {
                        if (song == artworkSong) setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                    .build()
                MediaItemData.Builder(Pair(song.id, index))
                    .setMediaItem(song.toMediaItem())
                    .setMediaMetadata(metadata)
                    .setDurationUs(song.duration.coerceAtLeast(0) * 1000)
                    .setIsSeekable(true)
                    .build()
            }
            cachedQueue = queue
        }
        val state = when {
            queue.isEmpty() -> STATE_IDLE
            service.playerState == PlayerState.STOPPED -> STATE_IDLE
            service.songDurationMillis >= 0 -> STATE_READY
            else -> STATE_IDLE
        }
        val position = service.songProgressMillis.toLong().coerceAtLeast(0)
        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(if (playlist.isEmpty()) C.INDEX_UNSET else queueManager.currentSongPosition.coerceIn(0, playlist.lastIndex))
            .setPlaybackState(state)
            .setPlayWhenReady(service.isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackParameters(PlaybackParameters(service.speed))
            .setRepeatMode(when (queueManager.repeatMode) {
                RepeatMode.NONE -> REPEAT_MODE_OFF
                RepeatMode.REPEAT_QUEUE -> REPEAT_MODE_ALL
                RepeatMode.REPEAT_SINGLE_SONG -> REPEAT_MODE_ONE
            })
            .setShuffleModeEnabled(queueManager.shuffleMode == ShuffleMode.SHUFFLE)
            .setSeekBackIncrementMs(service.seekJumpInterval.toLong())
            .setSeekForwardIncrementMs(service.seekJumpInterval.toLong())
            .setMaxSeekToPreviousPositionMs(5_000)
            .setContentPositionMs(position)
            .setContentBufferedPositionMs(PositionSupplier.getConstant(service.songDurationMillis.toLong().coerceAtLeast(position)))
            .build()
    }

    /** Complete only after the audio thread applied the command, not when it was enqueued. */
    private fun command(block: PlayerController.ControllerInternal.() -> Unit): ListenableFuture<*> {
        val result = SettableFuture.create<Void>()
        pendingCommands.add(result)
        result.addListener({ pendingCommands.remove(result) }, { mainHandler.post(it) })
        val handler = controller.handler
        if (!handler.post {
            try {
                if (!service.isDestroyed && !released) controller.executeSessionCommand(block)
                handler.post {
                    mainHandler.post {
                        refresh()
                        result.set(null)
                    }
                }
            } catch (e: Exception) {
                result.setException(e)
            }
        }) result.cancel(false)
        return result
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> = command {
        if (playWhenReady) play() else pause(true, PauseReason.PAUSE_BY_MANUAL_ACTION)
    }

    override fun handlePrepare(): ListenableFuture<*> = command {
        if (service.songDurationMillis < 0) prepareAt(queueManager.currentSongPosition, 0, false)
    }

    override fun handleStop(): ListenableFuture<*> = command { stop() }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> = command {
        if (queueManager.playingQueue.isNotEmpty()) {
            val current = queueManager.currentSongPosition
            val target = when (seekCommand) {
                COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> queueManager.nextLoopPosition
                COMMAND_SEEK_TO_PREVIOUS -> if (songProgressMillis > 5_000) current else queueManager.previousLoopPosition
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> queueManager.previousLoopPosition
                else -> mediaItemIndex
            }
            if (target in queueManager.playingQueue.indices) {
                val position = when (seekCommand) {
                    COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> 0L
                    else -> positionMs.takeUnless { it == C.TIME_UNSET } ?: 0L
                }.coerceAtLeast(0)
                if (target != current || service.songDurationMillis < 0) {
                    prepareAt(target, position, service.isPlaying)
                } else {
                    seekTo(position.coerceAtMost(songDurationMillis.toLong().coerceAtLeast(0)))
                }
            }
        }
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> = command {
        setPlayerSpeed(playbackParameters.speed)
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> = command {
        queueManager.modifyRepeatMode(when (repeatMode) {
            REPEAT_MODE_ONE -> RepeatMode.REPEAT_SINGLE_SONG
            REPEAT_MODE_ALL -> RepeatMode.REPEAT_QUEUE
            else -> RepeatMode.NONE
        }, false)
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> = command {
        val mode = if (shuffleModeEnabled) ShuffleMode.SHUFFLE else ShuffleMode.NONE
        if (queueManager.shuffleMode != mode) {
            queueManager.modifyShuffleMode(mode, alongWithQueue = queueManager.playingQueue.isNotEmpty(), async = false)
        }
    }

    private fun List<MediaItem>.songs(): List<Song> = map {
        requireNotNull(it.localConfiguration?.tag as? Song) { "Unresolved media item: ${it.mediaId}" }
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long,
    ): ListenableFuture<*> {
        val songs = mediaItems.songs()
        return command {
            if (songs.isEmpty()) {
                stop()
                queueManager.clearQueue(false)
            } else {
                val index = if (startIndex == C.INDEX_UNSET) 0 else startIndex
                require(index in songs.indices)
                val playing = service.isPlaying
                if (songs != queueManager.playingQueue) {
                    queueManager.swapQueue(songs, index, false)
                } else {
                    queueManager.modifyPosition(index, false)
                }
                prepareAt(queueManager.currentSongPosition, startPositionMs.takeUnless { it == C.TIME_UNSET } ?: 0, playing)
            }
        }
    }

    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {
        val songs = mediaItems.songs()
        return command {
            val empty = queueManager.playingQueue.isEmpty()
            queueManager.addSongs(songs, index, false)
            if (empty && queueManager.playingQueue.isNotEmpty()) queueManager.modifyPosition(0, false)
        }
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> = command {
        val count = toIndex - fromIndex
        if (newIndex < fromIndex) {
            repeat(count) { queueManager.moveSong(fromIndex + it, newIndex + it, false) }
        } else {
            repeat(count) { queueManager.moveSong(fromIndex, newIndex + count - 1, false) }
        }
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> = command {
        val current = queueManager.currentSongPosition
        val playing = service.isPlaying
        for (index in (fromIndex until toIndex).reversed()) queueManager.removeSongAt(index, false)
        if (queueManager.playingQueue.isEmpty()) stop()
        else if (current in fromIndex until toIndex) {
            val position = queueManager.currentSongPosition.coerceIn(queueManager.playingQueue.indices)
            prepareAt(position, 0, playing)
        }
    }

    override fun handleRelease(): ListenableFuture<*> {
        released = true
        pendingCommands.toList().forEach { it.cancel(false) }
        pendingCommands.clear()
        return Futures.immediateVoidFuture()
    }

    private val commands: Player.Commands = Player.Commands.Builder().addAll(
        COMMAND_PLAY_PAUSE, COMMAND_PREPARE, COMMAND_STOP, COMMAND_RELEASE,
        COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, COMMAND_SEEK_TO_MEDIA_ITEM,
        COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        COMMAND_SEEK_FORWARD, COMMAND_SEEK_BACK, COMMAND_SET_SPEED_AND_PITCH,
        COMMAND_SET_REPEAT_MODE, COMMAND_SET_SHUFFLE_MODE, COMMAND_SET_MEDIA_ITEM, COMMAND_CHANGE_MEDIA_ITEMS,
        COMMAND_GET_CURRENT_MEDIA_ITEM, COMMAND_GET_TIMELINE, COMMAND_GET_METADATA
    ).build()
}