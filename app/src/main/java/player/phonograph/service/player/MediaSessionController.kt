/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.player

import coil.request.Disposable
import player.phonograph.ACTUAL_PACKAGE_NAME
import player.phonograph.mechanism.setting.NotificationAction
import player.phonograph.mechanism.setting.NotificationActionsConfig
import player.phonograph.mechanism.setting.NotificationConfig
import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.service.MusicServiceStatus
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.browser.MediaBrowserDelegate
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.util.MediaButtonIntentReceiver
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_NUM_TRACKS
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_YEAR
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MediaSessionController : ServiceComponent {
    override var created: Boolean = false

    private var _service: MusicService? = null
    private val service: MusicService get() = _service!!


    private var _mediaSession: MediaSessionCompat? = null
    val mediaSession: MediaSessionCompat get() = _mediaSession!!

    override fun onCreate(musicService: MusicService) {
        _service = musicService

        updateCustomActions(NotificationConfig.actions)

        val mediaButtonReceiverComponentName = ComponentName(
            musicService.applicationContext,
            MediaButtonIntentReceiver::class.java
        )
        val mediaButtonReceiverPendingIntent =
            PendingIntent.getBroadcast(
                musicService.applicationContext,
                0,
                Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    component = mediaButtonReceiverComponentName
                },
                PendingIntent.FLAG_IMMUTABLE
            )

        _mediaSession =
            MediaSessionCompat(
                musicService,
                ACTUAL_PACKAGE_NAME,
                mediaButtonReceiverComponentName,
                mediaButtonReceiverPendingIntent
            )
        mediaSession.setMediaButtonReceiver(mediaButtonReceiverPendingIntent)

        mediaSession.setCallback(mediaSessionCallback)

        created = true

        service.coroutineScope.launch(SupervisorJob()) {
            Setting(musicService)[Keys.notificationActionsJsonString].flow.distinctUntilChanged().collect {
                updateCustomActions(NotificationConfig.actions)
            }
        }
    }

    override fun onDestroy(musicService: MusicService) {
        created = false
        disposable?.dispose()
        mediaSession.release()
        _mediaSession = null
        _service = null
    }


    private val sessionPlaybackStateBuilder
        get() = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
                    PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                    PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_SEEK_TO
        )

    private val mediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                service.play()
            }

            override fun onPause() {
                service.pause()
            }

            override fun onSkipToNext() {
                service.playNextSong(true)
            }

            override fun onSkipToPrevious() {
                service.back(true)
            }

            override fun onStop() {
                service.stopSelf()
            }

            override fun onSeekTo(pos: Long) {
                service.seek(pos.toInt())
            }

            val queueManager: QueueManager get() = service.queueManager

            override fun onSetShuffleMode(shuffleMode: Int) {
                when (shuffleMode) {
                    PlaybackStateCompat.SHUFFLE_MODE_INVALID -> {}
                    PlaybackStateCompat.SHUFFLE_MODE_NONE    -> queueManager.modifyShuffleMode(ShuffleMode.NONE)
                    PlaybackStateCompat.SHUFFLE_MODE_ALL     -> queueManager.modifyShuffleMode(ShuffleMode.SHUFFLE)
                    PlaybackStateCompat.SHUFFLE_MODE_GROUP   -> queueManager.modifyShuffleMode(ShuffleMode.SHUFFLE)
                }
            }

            override fun onSetRepeatMode(repeatMode: Int) {
                when (repeatMode) {
                    PlaybackStateCompat.REPEAT_MODE_INVALID -> {}
                    PlaybackStateCompat.REPEAT_MODE_ALL     -> queueManager.modifyRepeatMode(RepeatMode.REPEAT_QUEUE)
                    PlaybackStateCompat.REPEAT_MODE_GROUP   -> queueManager.modifyRepeatMode(RepeatMode.REPEAT_QUEUE)
                    PlaybackStateCompat.REPEAT_MODE_NONE    -> queueManager.modifyRepeatMode(RepeatMode.NONE)
                    PlaybackStateCompat.REPEAT_MODE_ONE     -> queueManager.modifyRepeatMode(RepeatMode.REPEAT_SINGLE_SONG)
                }
            }

            override fun onSetPlaybackSpeed(speed: Float) {
                service.speed = speed
            }

            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonIntentReceiver.handleIntent(service, mediaButtonEvent)
            }

            override fun onCustomAction(action: String?, extras: Bundle?) {
                service.processCommand(action)
            }

            override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
                service.coroutineScope.launch {
                    val request = MediaBrowserDelegate.playFromMediaId(service, mediaId, extras)
                    processRequest(request)
                }
            }

            override fun onPlayFromSearch(query: String?, extras: Bundle?) {
                service.coroutineScope.launch {
                    val request = MediaBrowserDelegate.playFromSearch(service, query, extras)
                    processRequest(request)
                }
            }

            private fun processRequest(request: PlayRequest) {
                when (request) {
                    PlayRequest.EmptyRequest     -> {}
                    is PlayRequest.PlayAtRequest -> service.playSongAt(request.position)
                    is PlayRequest.SongRequest   -> {
                        queueManager.addSong(request.song, queueManager.currentSongPosition, false)
                        service.playSongAt(queueManager.currentSongPosition)
                    }

                    is PlayRequest.SongsRequest  -> {
                        queueManager.swapQueue(request.songs, request.position, false)
                        service.playSongAt(0)
                    }
                }
            }
        }

    fun updatePlaybackState(status: MusicServiceStatus) {
        mediaSession.setPlaybackState(
            sessionPlaybackStateBuilder.setCustomActions(service, status)
                .setState(
                    if (status.isPlaying) STATE_PLAYING else STATE_PAUSED,
                    service.songProgressMillis.toLong(),
                    service.speed
                )
                .build()
        )
    }

    private fun PlaybackStateCompat.Builder.setCustomActions(musicService: MusicService, status: MusicServiceStatus):
            PlaybackStateCompat.Builder {
        for (action in customActions) {
            addCustomAction(
                action.action,
                musicService.getString(action.stringRes),
                action.icon(status)
            )
        }
        return this
    }

    private lateinit var customActions: List<NotificationAction>
    private fun updateCustomActions(config: NotificationActionsConfig) {
        customActions = config.actions.sortedBy { it.displayInCompat }.map { it.notificationAction }
            .filterNot { it in NotificationAction.COMMON_ACTIONS }
    }

    @Suppress("SameParameterValue")
    private fun fillMetadata(song: Song, pos: Long, total: Long, bitmap: Bitmap?) =
        MediaMetadataCompat.Builder().apply {
            putString(METADATA_KEY_TITLE, song.title)
            putLong(METADATA_KEY_DURATION, song.duration)
            putString(METADATA_KEY_ALBUM, song.albumName)
            putString(METADATA_KEY_ARTIST, song.artistName)
            putString(METADATA_KEY_ALBUM_ARTIST, song.artistName)
            putLong(METADATA_KEY_YEAR, song.year.toLong())
            putBitmap(METADATA_KEY_ALBUM_ART, bitmap)
            putLong(METADATA_KEY_TRACK_NUMBER, pos)
            putLong(METADATA_KEY_NUM_TRACKS, total)
        }

    private var disposable: Disposable? = null
    private var cachedBitmap: Bitmap? = null
    private var cachedSong: Song? = null
    fun updateMetaData(song: Song?, pos: Long, total: Long, loadCover: Boolean) {
        if (song == null) {
            mediaSession.setMetadata(null)
        } else {

            val metadata = fillMetadata(song, pos, total, null)
            if (loadCover && cachedSong == song && cachedBitmap != null) {
                metadata.putBitmap(METADATA_KEY_ALBUM_ART, cachedBitmap)
            }

            mediaSession.setMetadata(metadata.build())

            disposable?.dispose()
            if (loadCover && cachedSong != song) {
                disposable = service.coverLoader.load(song) { bitmap, _ ->
                    metadata.putBitmap(METADATA_KEY_ALBUM_ART, bitmap)
                    mediaSession.setMetadata(metadata.build())
                    this.cachedBitmap = bitmap
                    this.cachedSong = song
                }
            }
        }
    }

    companion object
}