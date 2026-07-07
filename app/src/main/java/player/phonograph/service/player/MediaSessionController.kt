/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.player

import coil.request.Disposable
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import player.phonograph.foundation.error.record
import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.notification.NotificationAction
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.model.service.MusicServiceStatus
import player.phonograph.repo.browser.MediaBrowserDelegate
import player.phonograph.repo.browser.MediaItemPath
import player.phonograph.repo.browser.toMediaItem
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.service.util.MediaButtonIntentReceiver
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingsObserver
import player.phonograph.ui.modules.main.MainActivity
import player.phonograph.ui.resource.Icons
import player.phonograph.ui.resource.Texts
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(UnstableApi::class)
class MediaSessionController : ServiceComponent {
    override var created: Boolean = false

    private var _service: MusicService? = null
    private val service: MusicService get() = _service!!
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var player: SessionPlayer
    private var _mediaSession: MediaLibrarySession? = null
    val mediaSession: MediaLibrarySession get() = _mediaSession!!
    private var settingsJob: Job? = null
    private var artworkJob: Job? = null
    private var coverRequest: Disposable? = null
    private var coverSong: Song? = null
    private var coverEnabled = false
    private var customActions: List<NotificationAction> = emptyList()

    override fun onCreate(musicService: MusicService) {
        _service = musicService
        player = SessionPlayer(musicService)
        _mediaSession = MediaLibrarySession.Builder(musicService, player, callback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    musicService, 0,
                    MainActivity.launchingIntent(musicService, Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
        created = true
        musicService.addSession(mediaSession)
        settingsJob = SettingsObserver(musicService, musicService.coroutineScope)
            .collect(Keys.notificationActions, Dispatchers.Main) { config ->
                if (created) {
                    updateCustomActions(config)
                    updateButtons(musicService.statusForNotification)
                }
            }
    }

    override fun onDestroy(musicService: MusicService) {
        created = false
        settingsJob?.cancel()
        artworkJob?.cancel()
        coverRequest?.dispose()
        mainHandler.removeCallbacksAndMessages(null)
        musicService.removeSession(mediaSession)
        mediaSession.release()
        player.release()
        _mediaSession = null
        _service = null
    }

    private fun onPlayerThread(block: () -> Unit) {
        if (Looper.myLooper() == mainHandler.looper) {
            if (created) block()
        } else {
            mainHandler.post { if (created) block() }
        }
    }

    fun updatePlaybackState(status: MusicServiceStatus) = onPlayerThread {
        player.refresh()
        updateButtons(status)
    }

    fun notifyQueueChanged() = onPlayerThread {
        player.refresh()
        mediaSession.notifyChildrenChanged(MediaItemPath.pageQueue.mediaId, service.queueManager.playingQueue.size, null)
    }

    private fun updateCustomActions(config: NotificationActionsConfig) {
        customActions = config.actions.sortedByDescending { it.displayInCompat }
            .map { it.notificationAction }
            .filterNot { it in NotificationAction.COMMON || it == NotificationAction.Invalid }
    }

    private fun updateButtons(status: MusicServiceStatus) {
        mediaSession.setMediaButtonPreferences(customActions.map { action ->
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setCustomIconResId(Icons.notificationAction(action, status))
                .setDisplayName(Texts.notificationAction(service.resources, action))
                .setSessionCommand(SessionCommand(action.command, Bundle.EMPTY))
                .build()
        })
    }

    fun updateMetaData(song: Song?, loadCover: Boolean) = onPlayerThread {
        player.refresh()
        if (coverSong == song && coverEnabled == loadCover) return@onPlayerThread
        coverRequest?.dispose()
        artworkJob?.cancel()
        coverSong = song
        coverEnabled = loadCover
        player.setArtwork(song, null)
        if (song != null && loadCover) {
            coverRequest = service.coverLoader.load(song) { bitmap, _ ->
                val musicService = _service ?: return@load
                artworkJob = musicService.coroutineScope.launch {
                    val bytes = bitmap?.let {
                        ByteArrayOutputStream().use { stream ->
                            val scale = 512f / maxOf(it.width, it.height)
                            val image = if (scale < 1) {
                                Bitmap.createScaledBitmap(
                                    it, (it.width * scale).toInt().coerceAtLeast(1),
                                    (it.height * scale).toInt().coerceAtLeast(1), true
                                )
                            } else it
                            image.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            stream.toByteArray()
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (created && coverSong == song && coverEnabled) player.setArtwork(song, bytes)
                    }
                }
            }
        }
    }

    /** Run library I/O off the application looper; complete callbacks on it. */
    private fun <T> libraryTask(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        val job = service.coroutineScope.launch {
            try {
                val value = block()
                withContext(Dispatchers.Main) { result.set(value) }
            } catch (e: CancellationException) {
                result.cancel(false)
                throw e
            } catch (e: Exception) {
                _service?.let { record(it, e, "MediaSessionController") }
                result.setException(e)
            }
        }
        result.addListener({ if (result.isCancelled) job.cancel() }, { it.run() })
        job.invokeOnCompletion { if (!result.isDone) result.cancel(false) }
        return result
    }

    private val callback = object : MediaLibrarySession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .apply { for (action in NotificationAction.ALL) add(SessionCommand(action.command, Bundle.EMPTY)) }
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(commands)
                .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                .build()
        }

        override fun onMediaButtonEvent(
            session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent,
        ): Boolean = MediaButtonIntentReceiver.handleIntent(service, intent)

        override fun onCustomCommand(
            session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (NotificationAction.ALL.none { it.command == customCommand.customAction }) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            service.processCommand(customCommand.customAction)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaBrowserDelegate.root(service, browser.packageName, browser.uid, params)
            return Futures.immediateFuture(
                if (root == null) LibraryResult.ofError(SessionError.ERROR_PERMISSION_DENIED)
                else LibraryResult.ofItem(root, params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String,
            page: Int, pageSize: Int, params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = libraryTask {
            LibraryResult.ofItemList(MediaBrowserDelegate.listChildren(parentId, service).page(page, pageSize), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = libraryTask {
            val item = MediaBrowserDelegate.item(service, mediaId)
            if (item == null) LibraryResult.ofError(SessionError.ERROR_BAD_VALUE) else LibraryResult.ofItem(item, null)
        }

        override fun onSearch(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = libraryTask {
            val songs = MediaBrowserDelegate.playFromSearch(service, query, params?.extras).songs
            withContext(Dispatchers.Main) {
                if (created) session.notifySearchResultChanged(browser, query, songs.size, params)
            }
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String,
            page: Int, pageSize: Int, params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = libraryTask {
            val songs = MediaBrowserDelegate.playFromSearch(service, query, params?.extras).songs
            LibraryResult.ofItemList(songs.page(page, pageSize).map { it.toMediaItem() }, params)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession, controller: MediaSession.ControllerInfo, isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val queue = service.queueManager.playingQueue
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    queue.map { it.toMediaItem() },
                    if (queue.isEmpty()) 0 else service.queueManager.currentSongPosition.coerceIn(queue.indices),
                    service.songProgressMillis.toLong().coerceAtLeast(0)
                )
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = Futures.transform(
            onSetMediaItems(mediaSession, controller, mediaItems, 0, C.TIME_UNSET),
            { it!!.mediaItems },
            { it.run() }
        )

        override fun onSetMediaItems(
            mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>,
            startIndex: Int, startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = libraryTask {
            val resolved = mutableListOf<MediaItem>()
            var resolvedIndex = 0
            val requestedIndex = if (startIndex == C.INDEX_UNSET) 0 else startIndex
            for ((index, item) in mediaItems.withIndex()) {
                val request = if (item.requestMetadata.searchQuery != null) {
                    MediaBrowserDelegate.playFromSearch(service, item.requestMetadata.searchQuery, item.requestMetadata.extras)
                } else {
                    MediaBrowserDelegate.playFromMediaId(service, item.mediaId, item.requestMetadata.extras)
                }
                val songs: List<Song>
                val position: Int
                when (request) {
                    PlayRequest.EmptyRequest     -> throw IllegalArgumentException("Unknown media item: ${item.mediaId}")
                    is PlayRequest.PlayAtRequest -> {
                        songs = service.queueManager.playingQueue.toList()
                        position = request.position
                    }
                    is PlayRequest.SongRequest   -> {
                        songs = listOf(request.song)
                        position = 0
                    }
                    is PlayRequest.SongsRequest  -> {
                        songs = request.songs
                        position = request.position
                    }
                }
                require(position in songs.indices) { "Empty or invalid playback request" }
                if (index == requestedIndex) resolvedIndex = resolved.size + position
                resolved += songs.map { it.toMediaItem() }
            }
            MediaSession.MediaItemsWithStartPosition(resolved, resolvedIndex, startPositionMs)
        }
    }

    companion object {
        private fun <T> List<T>.page(page: Int, pageSize: Int): List<T> {
            require(page >= 0 && pageSize > 0)
            val start = page.toLong() * pageSize
            if (start >= size) return emptyList()
            return subList(start.toInt(), (start + pageSize).coerceAtMost(size.toLong()).toInt())
        }
    }
}
