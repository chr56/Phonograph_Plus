/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.player

import coil.request.Disposable
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import player.phonograph.foundation.error.record
import player.phonograph.model.Song
import player.phonograph.model.notification.NotificationAction
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.model.service.MusicServiceStatus
import player.phonograph.model.service.PlayerState
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.browser.MediaBrowserDelegate
import player.phonograph.repo.browser.MediaBrowserTree
import player.phonograph.repo.browser.MediaItemPath
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingsObserver
import player.phonograph.ui.resource.Icons
import player.phonograph.ui.resource.Texts
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper

@OptIn(UnstableApi::class)
class MediaSessionController : ServiceComponent {
    override var created: Boolean = false

    private var _service: MusicService? = null
    private val service: MusicService get() = _service!!

    private var _player: DummyPlayer? = null
    private val player: DummyPlayer get() = _player!!

    private var _mediaSession: MediaLibrarySession? = null
    val mediaSession: MediaLibrarySession get() = _mediaSession!!

    override fun onCreate(musicService: MusicService) {
        _service = musicService
        _player = DummyPlayer(musicService, Looper.getMainLooper())
        _mediaSession = MediaLibrarySession.Builder(musicService, player, mediaLibrarySessionCallback)
            .setMediaButtonPreferences(commandButtons(musicService.statusForNotification))
            .build()

        created = true

        val settingsObserver = SettingsObserver(musicService, musicService.coroutineScope)
        settingsObserver.collect(Keys.notificationActions) { config ->
            updateCustomActions(config)
            mediaSession.setMediaButtonPreferences(commandButtons(musicService.statusForNotification))
        }

    }

    override fun onDestroy(musicService: MusicService) {
        created = false
        disposable?.dispose()
        mediaSession.release()
        player.release()
        _mediaSession = null
        _player = null
        _service = null
    }

    fun updatePlaybackState(status: MusicServiceStatus) {
        player.refresh()
        mediaSession.setMediaButtonPreferences(commandButtons(status))
    }

    private var customActions: List<NotificationAction> = emptyList()
    private fun updateCustomActions(config: NotificationActionsConfig) {
        customActions = config.actions.sortedBy { it.displayInCompat }.map { it.notificationAction }
            .filterNot { it in NotificationAction.COMMON }
    }

    private fun commandButtons(status: MusicServiceStatus): List<CommandButton> {
        return customActions.map { action ->
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setCustomIconResId(Icons.notificationAction(action, status))
                .setDisplayName(Texts.notificationAction(service.resources, action))
                .setSessionCommand(SessionCommand(action.command, Bundle.EMPTY))
                .build()
        }
    }

    private val mediaLibrarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = SessionCommands.Builder()
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT)
                .apply {
                    for (action in NotificationAction.ALL) {
                        add(SessionCommand(action.command, Bundle.EMPTY))
                    }
                }
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            service.processCommand(customCommand.customAction)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaBrowserDelegate.root(
                service,
                browser.packageName,
                browser.uid,
                params
            ) ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val mediaItems = runCatching {
                kotlinx.coroutines.runBlocking {
                    MediaBrowserDelegate.listChildren(parentId, service)
                }
            }.getOrElse { error ->
                record(service, error, javaClass.name)
                MediaBrowserDelegate.error(service)
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(mediaItems, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val mediaItemPath = MediaBrowserTree.resolve(mediaId)
            val segments = mediaItemPath?.segments.orEmpty()
            val playable = when {
                segments.getOrNull(1) == MediaItemPath.PLAY_ALL                          -> true
                segments.firstOrNull() == MediaItemPath.SONGS && segments.size > 1       -> true
                segments.firstOrNull() == MediaItemPath.SONGS_QUEUE && segments.size > 1 -> true
                else                                                                     -> false
            }
            val browsable = !playable
            val item = MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(browsable)
                        .setIsPlayable(playable)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }
    }

    fun updateMetaData(song: Song?, pos: Long, total: Long, loadCover: Boolean) {
        player.refresh()
        if (song == null) return

        val metadata = song.toMediaMetadata(pos, total, null)
        player.currentMetadata = metadata
        player.refresh()

        disposable?.dispose()
        if (loadCover && cachedSong == song && cachedBitmap != null) {
            player.currentMetadata = song.toMediaMetadata(pos, total, cachedBitmap)
            player.refresh()
        } else if (loadCover && cachedSong != song) {
            disposable = service.coverLoader.load(song) { bitmap, _ ->
                cachedBitmap = bitmap
                cachedSong = song
                player.currentMetadata = song.toMediaMetadata(pos, total, bitmap)
                player.refresh()
            }
        }
    }

    private var disposable: Disposable? = null
    private var cachedBitmap: Bitmap? = null
    private var cachedSong: Song? = null

    private fun Song.toMediaMetadata(pos: Long, total: Long, bitmap: Bitmap?): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(title)
            .setDurationMs(duration)
            .setAlbumTitle(albumName)
            .setArtist(artistName)
            .setAlbumArtist(artistName)
            .setTrackNumber(pos.toInt())
            .setTotalTrackCount(total.toInt())
            .setArtworkData(bitmap?.toByteArray(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    /**
     * A dummy player used for Media3
     */
    @OptIn(UnstableApi::class)
    private inner class DummyPlayer(private val musicService: MusicService, looper: Looper) :
            SimpleBasePlayer(looper) {

        var currentMetadata: MediaMetadata = MediaMetadata.EMPTY

        fun refresh() = invalidateState()

        override fun getState(): State {
            val queueManager = musicService.queueManager
            val queue = queueManager.playingQueue
            val currentIndex = queueManager.currentSongPosition.coerceAtLeast(0)
            val playlist = buildPlaylist(queue, currentIndex)
            return State.Builder()
                .setAvailableCommands(availableCommands)
                .setPlaybackState(currentPlaybackState)
                .setPlayWhenReady(musicService.isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlaybackParameters(PlaybackParameters(musicService.speed))
                .setRepeatMode(queueManager.repeatMode.toPlayerRepeatMode())
                .setShuffleModeEnabled(queueManager.shuffleMode == ShuffleMode.SHUFFLE)
                .setPlaylist(playlist)
                .setCurrentMediaItemIndex(if (playlist.isEmpty()) C.INDEX_UNSET else currentIndex.coerceAtMost(playlist.lastIndex))
                .setContentPositionMs(musicService.songProgressMillis.toLong())
                .setContentBufferedPositionMs(PositionSupplier.getConstant(musicService.songProgressMillis.toLong()))
                .setTotalBufferedDurationMs(PositionSupplier.ZERO)
                .build()
        }

        private fun buildPlaylist(
            queue: List<Song>, current: Int,
        ): List<MediaItemData> = queue.mapIndexed { index, song ->
            val metadata = if (index == current) currentMetadata else song.toMediaMetadata(0, queue.size.toLong(), null)
            val mediaItem = MediaItem.Builder()
                .setMediaId(MediaItemPath.song(song.id).mediaId)
                .setMediaMetadata(metadata)
                .build()
            MediaItemData.Builder(song.id)
                .setMediaItem(mediaItem)
                .setMediaMetadata(metadata)
                .setDurationUs(song.duration * 1000)
                .setIsSeekable(true)
                .build()
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            if (playWhenReady) musicService.play() else musicService.pause()
            return Futures.immediateVoidFuture()
        }

        override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
            when (seekCommand) {
                COMMAND_SEEK_FORWARD                -> musicService.fastForward()
                COMMAND_SEEK_BACK                   -> musicService.fastRewind()
                COMMAND_SEEK_TO_PREVIOUS            -> musicService.back(true)
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> musicService.back(false)
                COMMAND_SEEK_TO_NEXT                -> musicService.playNextSong(true)
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM     -> musicService.playNextSong(false)
                COMMAND_SEEK_TO_MEDIA_ITEM          -> musicService.playSongAt(mediaItemIndex)
                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM  -> musicService.seek(positionMs.toInt())
                else                                -> musicService.seek(positionMs.toInt())
            }
            return Futures.immediateVoidFuture()
        }

        override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
            musicService.speed = playbackParameters.speed
            return Futures.immediateVoidFuture()
        }

        override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
            musicService.queueManager.modifyRepeatMode(repeatMode.toRepeatMode())
            return Futures.immediateVoidFuture()
        }

        override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
            musicService.queueManager.modifyShuffleMode(if (shuffleModeEnabled) ShuffleMode.SHUFFLE else ShuffleMode.NONE)
            return Futures.immediateVoidFuture()
        }

        override fun handleStop(): ListenableFuture<*> {
            musicService.stopSelf()
            return Futures.immediateVoidFuture()
        }

        override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

        private val currentPlaybackState: Int
            get() = when (musicService.playerState) {
                PlayerState.PLAYING -> STATE_READY
                PlayerState.PAUSED  -> STATE_READY
                PlayerState.STOPPED -> STATE_IDLE
                else                -> STATE_BUFFERING
            }

        private val availableCommands: Player.Commands =
            Player.Commands.Builder()
                .add(COMMAND_PLAY_PAUSE)
                .add(COMMAND_STOP)
                .add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(COMMAND_SEEK_TO_NEXT)
                .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(COMMAND_SEEK_TO_PREVIOUS)
                .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(COMMAND_SEEK_FORWARD)
                .add(COMMAND_SEEK_BACK)
                .add(COMMAND_SEEK_TO_MEDIA_ITEM)
                .add(COMMAND_SET_SPEED_AND_PITCH)
                .add(COMMAND_SET_REPEAT_MODE)
                .add(COMMAND_SET_SHUFFLE_MODE)
                .add(COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(COMMAND_GET_TIMELINE)
                .add(COMMAND_GET_METADATA)
                .build()


        private fun RepeatMode.toPlayerRepeatMode(): Int =
            when (this) {
                RepeatMode.NONE               -> REPEAT_MODE_OFF
                RepeatMode.REPEAT_SINGLE_SONG -> REPEAT_MODE_ONE
                RepeatMode.REPEAT_QUEUE       -> REPEAT_MODE_ALL
            }

        private fun Int.toRepeatMode(): RepeatMode =
            when (this) {
                REPEAT_MODE_ONE -> RepeatMode.REPEAT_SINGLE_SONG
                REPEAT_MODE_ALL -> RepeatMode.REPEAT_QUEUE
                else            -> RepeatMode.NONE
            }

    }
}