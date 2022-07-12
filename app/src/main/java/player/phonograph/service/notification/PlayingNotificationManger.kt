/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata.*
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import player.phonograph.BuildConfig
import player.phonograph.glide.BlurTransformation
import player.phonograph.glide.SongGlideRequest
import player.phonograph.service.MusicService
import player.phonograph.service.util.MediaButtonIntentReceiver
import player.phonograph.settings.Setting
import player.phonograph.util.ImageUtil.copy
import player.phonograph.util.Util

class PlayingNotificationManger(private val service: MusicService) {

    lateinit var playingNotification: PlayingNotification

    @SuppressLint("ObsoleteSdkInt")
    fun setUpNotification() {
        playingNotification = if (!Setting.instance.classicNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PlayingNotificationImpl24(service)
        } else {
            PlayingNotificationImpl(service)
        }
    }

    fun removeNotification() = playingNotification.stop()

    fun updateNotification() {
        val song = service.queueManager.currentSong
        if (song.id != -1L) {
            playingNotification.metaData = PlayingNotification.SongMetaData(song)
        }
    }

    lateinit var mediaSession: MediaSessionCompat private set

    fun setupMediaSession(callback: MediaSessionCompat.Callback) {
        val mediaButtonReceiverComponentName = ComponentName(
            service.applicationContext,
            MediaButtonIntentReceiver::class.java
        )
        val mediaButtonReceiverPendingIntent =
            PendingIntent.getBroadcast(
                service.applicationContext,
                0,
                Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    component = mediaButtonReceiverComponentName
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        mediaSession =
            MediaSessionCompat(
                service,
                BuildConfig.APPLICATION_ID,
                mediaButtonReceiverComponentName,
                mediaButtonReceiverPendingIntent
            )
        mediaSession.setCallback(callback)

        // fixme remove deprecation
        @Suppress("DEPRECATION")
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
        )
        mediaSession.setMediaButtonReceiver(mediaButtonReceiverPendingIntent)
    }

    fun updateMediaSessionPlaybackState() {
        mediaSession.setPlaybackState(
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
                    if (service.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    service.songProgressMillis.toLong(),
                    1f
                )
                .build()
        )
    }

    @SuppressLint("CheckResult")
    fun updateMediaSessionMetaData() {
        val queueManager = service.queueManager
        val song = queueManager.currentSong
        if (song.id == -1L) {
            mediaSession.setMetadata(null)
            return
        }
        val metaData =
            MediaMetadataCompat.Builder().apply {
                putString(METADATA_KEY_ARTIST, song.artistName)
                putString(METADATA_KEY_ALBUM_ARTIST, song.artistName)
                putString(METADATA_KEY_ALBUM, song.albumName)
                putString(METADATA_KEY_TITLE, song.title)
                putLong(METADATA_KEY_DURATION, song.duration)
                putLong(METADATA_KEY_TRACK_NUMBER, (queueManager.currentSongPosition + 1).toLong())
                putLong(METADATA_KEY_YEAR, song.year.toLong())
                putBitmap(METADATA_KEY_ALBUM_ART, null)
                putLong(
                    METADATA_KEY_NUM_TRACKS,
                    service.queueManager.playingQueue.size.toLong()
                )
            }

        if (Setting.instance.albumArtOnLockscreen) {
            val screenSize = Util.getScreenSize(service)
            val request = SongGlideRequest.Builder.from(Glide.with(service), song)
                .checkIgnoreMediaStore(service)
                .asBitmap().build().also {
                    if (Setting.instance.blurredAlbumArt) it.transform(
                        BlurTransformation.Builder(service).build()
                    )
                }

            service.runOnUiThread {
                request.into(
                    object : CustomTarget<Bitmap>(screenSize.x, screenSize.y) {

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            mediaSession.setMetadata(metaData.build())
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            metaData.putBitmap(
                                METADATA_KEY_ALBUM_ART,
                                resource.copy()
                            )
                            mediaSession.setMetadata(metaData.build())
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            mediaSession.setMetadata(metaData.build()) // todo check leakage
                        }
                    }
                )
            }
        } else {
            mediaSession.setMetadata(metaData.build())
        }
    }
}
