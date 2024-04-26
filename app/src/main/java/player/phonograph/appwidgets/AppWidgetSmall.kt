package player.phonograph.appwidgets

import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.appwidgets.Util.createRoundedBitmap
import player.phonograph.appwidgets.base.BaseAppWidget
import player.phonograph.coil.target.PaletteTargetBuilder
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews

class AppWidgetSmall : BaseAppWidget() {

    override val layoutId: Int get() = R.layout.app_widget_small

    override val name: String = NAME

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(context: Context, isPlaying: Boolean, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(context.packageName, R.layout.app_widget_small)
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
        setupDefaultPhonographWidgetButtons(context, appWidgetView)
        setupLaunchingClick(context, appWidgetView)
        if (imageSize == 0) imageSize = context.resources.getDimensionPixelSize(
            R.dimen.app_widget_small_image_size
        )
        if (cardRadius == 0f) cardRadius = context.resources.getDimension(
            R.dimen.app_widget_card_radius
        )
        val fallbackColor: Int = context.secondaryTextColor(false)

        // Load the album cover async and push the update on completion
        loadImage(
            context = context.applicationContext,
            song = song,
            widgetImageSize = imageSize,
            target =
            PaletteTargetBuilder(fallbackColor)
                .onStart {
                    appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art)
                }
                .onResourceReady { result, paletteColor ->
                    updateWidget(appWidgetView, context, isPlaying, result.toBitmapOrNull(), paletteColor)
                    pushUpdate(context, appWidgetIds, appWidgetView)
                }
                .onFail {
                    updateWidget(appWidgetView, context, isPlaying, null, fallbackColor)
                    pushUpdate(context, appWidgetIds, appWidgetView)
                }
                .build()
        )
    }

    private fun updateWidget(
        appWidgetView: RemoteViews,
        context: Context,
        isPlaying: Boolean,
        bitmap: Bitmap?,
        color: Int,
    ) {

        appWidgetView.bindDrawable(context, R.id.button_toggle_play_pause, playPauseRes(isPlaying), color)
        appWidgetView.bindDrawable(context, R.id.button_next, R.drawable.ic_skip_next_white_24dp, color)
        appWidgetView.bindDrawable(context, R.id.button_prev, R.drawable.ic_skip_previous_white_24dp, color)

        appWidgetView.setImageViewBitmap(
            R.id.image,
            createRoundedBitmap(
                getAlbumArtDrawable(context.resources, bitmap),
                imageSize,
                imageSize,
                cardRadius,
                0f,
                cardRadius,
                0f
            )
        )
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
