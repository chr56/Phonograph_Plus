package player.phonograph.appwidgets

import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.appwidgets.Util.createRoundedBitmap
import player.phonograph.appwidgets.base.BaseAppWidget
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.service.MusicService
import player.phonograph.util.theme.createTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews

class AppWidgetSmall : BaseAppWidget() {

    override val layoutId: Int get() = R.layout.app_widget_small

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(service.packageName, R.layout.app_widget_small)
        val isPlaying = service.isPlaying
        val song = queueManager.currentSong

        // Set the titles and artwork
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            if (TextUtils.isEmpty(song.title) || TextUtils.isEmpty(song.artistName)) {
                appWidgetView.setTextViewText(R.id.text_separator, "")
            } else {
                appWidgetView.setTextViewText(R.id.text_separator, "•")
            }
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE)
            appWidgetView.setTextViewText(R.id.title, song.title)
            appWidgetView.setTextViewText(R.id.text, song.artistName)
        }

        // Link actions buttons to intents
        setupDefaultPhonographWidgetButtons(service, appWidgetView)
        setupLaunchingClick(service, appWidgetView)
        if (imageSize == 0) imageSize = service.resources.getDimensionPixelSize(
            R.dimen.app_widget_small_image_size
        )
        if (cardRadius == 0f) cardRadius = service.resources.getDimension(
            R.dimen.app_widget_card_radius
        )
        val fallbackColor: Int = service.secondaryTextColor(false)

        // Load the album cover async and push the update on completion
        loadImage(
            context = service.applicationContext,
            song = song,
            widgetImageSize = imageSize,
            target =
            PaletteTargetBuilder(fallbackColor)
                .onStart {
                    appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art)
                }
                .onResourceReady { result, paletteColor ->
                    updateWidget(appWidgetView, service, isPlaying, result.toBitmapOrNull(), paletteColor)
                    pushUpdate(service, appWidgetIds, appWidgetView)
                }
                .onFail {
                    updateWidget(appWidgetView, service, isPlaying, null, fallbackColor)
                    pushUpdate(service, appWidgetIds, appWidgetView)
                }
                .build()
        )
    }

    private fun updateWidget(appWidgetView: RemoteViews, service: MusicService, isPlaying: Boolean, bitmap: Bitmap?, color: Int) {
        // Set correct drawable for pause state
        val playPauseRes = if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause,
            BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    playPauseRes,
                    color
                )!!
            )
        )

        // Set prev/next button drawables
        appWidgetView.setImageViewBitmap(
            R.id.button_next,
            BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    R.drawable.ic_skip_next_white_24dp,
                    color
                )!!
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev,
            BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    R.drawable.ic_skip_previous_white_24dp,
                    color
                )!!
            )
        )
        val image = getAlbumArtDrawable(service.resources, bitmap)
        val roundedBitmap = createRoundedBitmap(
            image,
            imageSize,
            imageSize,
            cardRadius,
            0f,
            0f,
            0f
        )
        appWidgetView.setImageViewBitmap(R.id.image, roundedBitmap)
    }

    override fun setupLaunchingClick(context: Context, view: RemoteViews) {
        view.setOnClickPendingIntent(R.id.image, launchPendingIntent(context))
        view.setOnClickPendingIntent(R.id.media_titles, launchPendingIntent(context))
    }

    companion object {
        const val NAME = "app_widget_small"
        private var mInstance: AppWidgetSmall? = null
        private var imageSize = 0
        private var cardRadius = 0f

        @JvmStatic
        @get:Synchronized
        val instance: AppWidgetSmall
            get() {
                if (mInstance == null) {
                    mInstance = AppWidgetSmall()
                }
                return mInstance!!
            }
    }
}
