package player.phonograph.notification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import player.phonograph.R
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.palette.BitmapPaletteWrapper
import player.phonograph.service.MusicService
import player.phonograph.settings.Setting
import player.phonograph.util.PhonographColorUtil
import androidx.media.app.NotificationCompat as MediaNotificationCompat

class PlayingNotificationImpl24(service: MusicService) : PlayingNotification(service) {

    @Synchronized
    override fun update() {

        metaData?.let {
            val song = it.song
            val isPlaying = service.isPlaying

            val bigNotificationImageSize = service.resources.getDimensionPixelSize(R.dimen.notification_big_image_size)

            val playPauseAction = NotificationCompat.Action(
                if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp,
                service.getString(R.string.action_play_pause),
                buildPlaybackPendingIntent(MusicService.ACTION_TOGGLE_PAUSE)
            )
            val previousAction = NotificationCompat.Action(
                R.drawable.ic_skip_previous_white_24dp,
                service.getString(R.string.action_previous),
                buildPlaybackPendingIntent(MusicService.ACTION_REWIND)
            )
            val nextAction = NotificationCompat.Action(
                R.drawable.ic_skip_next_white_24dp,
                service.getString(R.string.action_next),
                buildPlaybackPendingIntent(MusicService.ACTION_SKIP)
            )

            val defaultCover = BitmapFactory.decodeResource(service.resources, R.drawable.default_album_art)

            notificationBuilder
                .setContentTitle(song.title)
                .setContentText(song.artistName)
                .setSubText(song.albumName)
                .setOngoing(isPlaying)
                .setLargeIcon(defaultCover)
                .clearActions()
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .also { builder ->
                    /* noinspection ObsoleteSdkInt*/
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder
                            .setStyle(
                                MediaNotificationCompat.MediaStyle()
                                    .setMediaSession(service.mediaSession!!.sessionToken)
                                    .setShowActionsInCompactView(0, 1, 2)
                            )
                    }
                }

            updateNotification(notificationBuilder.build())

            // then try to load cover image

            service.runOnUiThread {
                SongGlideRequest.Builder.from(Glide.with(service), song)
                    .checkIgnoreMediaStore(service)
                    .generatePalette(service).build()
                    .into(object : CustomTarget<BitmapPaletteWrapper>(bigNotificationImageSize, bigNotificationImageSize) {

                        override fun onResourceReady(resource: BitmapPaletteWrapper, transition: Transition<in BitmapPaletteWrapper>?) {
                            updateCover(resource.bitmap, PhonographColorUtil.getColor(resource.palette, Color.TRANSPARENT))
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            updateCover(null, Color.TRANSPARENT)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            updateCover(null, Color.WHITE)
                        }

                        fun updateCover(bitmap: Bitmap?, color: Int) {
                            notificationBuilder
                                .setLargeIcon(bitmap ?: defaultCover)
                                .also { builder ->
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O && Setting.instance.coloredNotification) builder.color = color
                                }
                            updateNotification(notificationBuilder.build())
                        }
                    })
            }
        }
    }
}
