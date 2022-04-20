package player.phonograph.notification

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import player.phonograph.R
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.palette.BitmapPaletteWrapper
import player.phonograph.service.MusicService
import player.phonograph.settings.Setting
import player.phonograph.util.ImageUtil
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.ColorUtil
import util.mddesign.util.MaterialColorHelper

class PlayingNotificationImpl(service: MusicService) : PlayingNotification(service) {

    private var target: Target<BitmapPaletteWrapper>? = null

    @Synchronized
    override fun update() {
        metaData?.let {
            val song = it.song
            val isPlaying = service.isPlaying
            val notificationLayout = RemoteViews(service.packageName, R.layout.notification)
            val notificationLayoutBig = RemoteViews(service.packageName, R.layout.notification_big)

            if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
                notificationLayout.setViewVisibility(R.id.media_titles, View.INVISIBLE)
            } else {
                notificationLayout.setViewVisibility(R.id.media_titles, View.VISIBLE)
                notificationLayout.setTextViewText(R.id.title, song.title)
                notificationLayout.setTextViewText(R.id.text, song.artistName)
            }

            if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName) && TextUtils.isEmpty(song.albumName)) {
                notificationLayoutBig.setViewVisibility(R.id.media_titles, View.INVISIBLE)
            } else {
                notificationLayoutBig.setViewVisibility(R.id.media_titles, View.VISIBLE)
                notificationLayoutBig.setTextViewText(R.id.title, song.title)
                notificationLayoutBig.setTextViewText(R.id.text, song.artistName)
                notificationLayoutBig.setTextViewText(R.id.text2, song.albumName)
            }

            linkButtons(notificationLayout, notificationLayoutBig)

            notificationBuilder
                .setContent(notificationLayout)
                .setCustomBigContentView(notificationLayoutBig)
                .setOngoing(isPlaying)

            updateNotification(notificationBuilder.build())

            // then try to load cover image

            service.runOnUiThread {
                if (target != null) {
                    Glide.with(service).clear(target)
                }

                target = SongGlideRequest.Builder.from(Glide.with(service), song)
                    .checkIgnoreMediaStore(service).generatePalette(service).build()
                    .into(object : CustomTarget<BitmapPaletteWrapper>(bigNotificationImageSize, bigNotificationImageSize) {

                        override fun onResourceReady(resource: BitmapPaletteWrapper, transition: Transition<in BitmapPaletteWrapper>?) {
                            updateCover(resource.bitmap, PhonographColorUtil.getColor(resource.palette, Color.TRANSPARENT))
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            updateCover(null, Color.WHITE)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            updateCover(null, Color.WHITE)
                        }

                        private fun updateCover(bitmap: Bitmap?, backgroundColor: Int) {
                            val color = if (Setting.instance.coloredNotification) backgroundColor else Color.WHITE
                            if (bitmap != null) {
                                notificationLayout.setImageViewBitmap(R.id.image, bitmap)
                                notificationLayoutBig.setImageViewBitmap(R.id.image, bitmap)
                            } else {
                                notificationLayout.setImageViewResource(R.id.image, R.drawable.default_album_art)
                                notificationLayoutBig.setImageViewResource(R.id.image, R.drawable.default_album_art)
                            }
                            setBackgroundColor(color)
                            setNotificationContent(ColorUtil.isColorLight(color))

                            updateNotification(notificationBuilder.build())
                        }

                        private fun setBackgroundColor(color: Int) {
                            notificationLayout.setInt(R.id.root, "setBackgroundColor", color)
                            notificationLayoutBig.setInt(R.id.root, "setBackgroundColor", color)
                        }

                        private fun setNotificationContent(dark: Boolean) {
                            val primary = MaterialColorHelper.getPrimaryTextColor(service, dark)
                            val secondary = MaterialColorHelper.getSecondaryTextColor(service, dark)

                            val prev = ImageUtil.createBitmap(
                                ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_previous_white_24dp, primary), 1.5f
                            )
                            val next = ImageUtil.createBitmap(
                                ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_next_white_24dp, primary), 1.5f
                            )
                            val playPause = ImageUtil.createBitmap(
                                ImageUtil.getTintedVectorDrawable(
                                    service,
                                    if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp,
                                    primary
                                ),
                                1.5f
                            )

                            notificationLayout.setTextColor(R.id.title, primary)
                            notificationLayout.setTextColor(R.id.text, secondary)

                            notificationLayout.setImageViewBitmap(R.id.action_prev, prev)
                            notificationLayout.setImageViewBitmap(R.id.action_next, next)
                            notificationLayout.setImageViewBitmap(R.id.action_play_pause, playPause)

                            notificationLayoutBig.setTextColor(R.id.title, primary)
                            notificationLayoutBig.setTextColor(R.id.text, secondary)
                            notificationLayoutBig.setTextColor(R.id.text2, secondary)

                            notificationLayoutBig.setImageViewBitmap(R.id.action_prev, prev)
                            notificationLayoutBig.setImageViewBitmap(R.id.action_next, next)
                            notificationLayoutBig.setImageViewBitmap(R.id.action_play_pause, playPause)
                        }
                    })
            }
        }
    }

    private fun linkButtons(notificationLayout: RemoteViews, notificationLayoutBig: RemoteViews) {

        @Suppress("JoinDeclarationAndAssignment")
        var pendingIntent: PendingIntent

        // Previous track
        pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_REWIND)
        notificationLayout.setOnClickPendingIntent(R.id.action_prev, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_TOGGLE_PAUSE)
        notificationLayout.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_SKIP)
        notificationLayout.setOnClickPendingIntent(R.id.action_next, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_next, pendingIntent)
    }

    val bigNotificationImageSize by lazy { service.resources.getDimensionPixelSize(R.dimen.notification_big_image_size) }
}
