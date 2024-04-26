package player.phonograph.appwidgets


import coil.target.Target
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.appwidgets.base.BaseAppWidget
import player.phonograph.service.MusicService
import player.phonograph.util.theme.createTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import player.phonograph.util.ui.getScreenSize
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews


class AppWidgetBig : BaseAppWidget() {

    override val layoutId: Int get() = R.layout.app_widget_big

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(service.packageName, R.layout.app_widget_big)
        val isPlaying = service.isPlaying
        val song = queueManager.currentSong

        // Set the titles and artwork
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE)
            appWidgetView.setTextViewText(R.id.title, song.title)
            appWidgetView.setTextViewText(R.id.text, getSongArtistAndAlbum(song))
        }

        // Set correct drawable for pause state
        val playPauseRes = if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause,
            BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    playPauseRes,
                    service.primaryTextColor(true)
                )!!
            )
        )

        // Set prev/next button drawables
        appWidgetView.setImageViewBitmap(
            R.id.button_next,
            BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    R.drawable.ic_skip_next_white_24dp,
                    service.primaryTextColor(true)
                )!!
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev,
            BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    R.drawable.ic_skip_previous_white_24dp,
                    service.primaryTextColor(true)
                )!!
            )
        )

        // Link actions buttons to intents
        setupDefaultPhonographWidgetButtons(service, appWidgetView)
        setupLaunchingClick(service, appWidgetView)

        // Load the album cover async and push the update on completion
        val p = service.getScreenSize()
        val widgetImageSize = p.x.coerceAtMost(p.y)
        loadImage(
            context = service.applicationContext,
            song = song,
            widgetImageSize = widgetImageSize,
            target =
            object : Target {
                val mainHandler: Handler = Handler(Looper.getMainLooper())
                override fun onStart(placeholder: Drawable?) {
                    mainHandler.post { onUpdate(null) }
                }

                override fun onError(error: Drawable?) {
                    mainHandler.post { onUpdate(null) }
                }

                override fun onSuccess(result: Drawable) {
                    mainHandler.post { onUpdate(result.toBitmapOrNull()) }
                }

                private fun onUpdate(bitmap: Bitmap?) {
                    if (bitmap == null) {
                        appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art)
                    } else {
                        appWidgetView.setImageViewBitmap(R.id.image, bitmap)
                    }
                    pushUpdate(service.applicationContext, appWidgetIds, appWidgetView)
                }
            }
        )


    }

    override fun setupLaunchingClick(context: Context, view: RemoteViews) {
        view.setOnClickPendingIntent(R.id.clickable_area, launchPendingIntent(context))
    }

    companion object {
        const val NAME = "app_widget_big"
        private var mInstance: AppWidgetBig? = null

        @JvmStatic
        @get:Synchronized
        val instance: AppWidgetBig
            get() {
                if (mInstance == null) {
                    mInstance = AppWidgetBig()
                }
                return mInstance!!
            }
    }
}
