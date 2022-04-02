package player.phonograph.notification

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
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
import player.phonograph.ui.activities.MainActivity

class PlayingNotificationImpl24(service: MusicService) : PlayingNotification(service) {

    @Synchronized
    override fun update() {
        stopped = false

        val song = service.currentSong
        val isPlaying = service.isPlaying

        val clickPendingIntent = PendingIntent.getActivity(
            service, 0,
            Intent(service, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val deletePendingIntent = PendingIntent.getService(
            service, 0,
            Intent(MusicService.ACTION_QUIT).apply { component = ComponentName(service, MusicService::class.java) },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        val bigNotificationImageSize = service.resources.getDimensionPixelSize(R.dimen.notification_big_image_size)

        service.runOnUiThread {
            SongGlideRequest.Builder.from(Glide.with(service), song)
                .checkIgnoreMediaStore(service)
                .generatePalette(service).build()
                .into(object : CustomTarget<BitmapPaletteWrapper>(bigNotificationImageSize, bigNotificationImageSize) {

                    override fun onResourceReady(resource: BitmapPaletteWrapper, transition: Transition<in BitmapPaletteWrapper>?) {
                        val palette = resource.palette
                        update(resource.bitmap, palette.getVibrantColor(palette.getMutedColor(Color.TRANSPARENT)))
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        update(null, Color.TRANSPARENT)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        update(null, Color.WHITE)
                    }

                    fun update(bitmap: Bitmap?, color: Int) {

                        val coverImage = bitmap ?: BitmapFactory.decodeResource(service.resources, R.drawable.default_album_art)

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

                        val notificationBuilder = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setLargeIcon(coverImage)
                            .setContentTitle(song.title)
                            .setContentText(song.artistName)
                            .setSubText(song.albumName)
                            .setOngoing(isPlaying)
                            .setContentIntent(clickPendingIntent)
                            .setDeleteIntent(deletePendingIntent)
                            .addAction(previousAction)
                            .addAction(playPauseAction)
                            .addAction(nextAction)
                            .setShowWhen(false)
                            .also { builder ->
                                /* noinspection ObsoleteSdkInt*/
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    builder
                                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                        .setStyle(
                                            androidx.media.app.NotificationCompat.MediaStyle()
                                                .setMediaSession(service.mediaSession.sessionToken)
                                                .setShowActionsInCompactView(0, 1, 2)
                                        )
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O && Setting.instance.coloredNotification) builder.color = color
                                }
                            }

                        if (!stopped) updateNotifyModeAndPostNotification(notificationBuilder.build()) // notification has been stopped before loading was finished
                    }
                })
        }
    }
}
