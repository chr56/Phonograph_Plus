package player.phonograph.appwidgets

import player.phonograph.R
import player.phonograph.appwidgets.Util.createRoundedBitmap
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import util.theme.color.secondaryTextColor
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews

class AppWidgetCard : BaseAppWidget() {

    override val layoutId: Int get() = R.layout.app_widget_card

    override val name: String = NAME

    override val darkBackground: Boolean get() = false


    override fun updateText(context: Context, view: RemoteViews, song: Song) {
        // Set the titles and artwork
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            view.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            view.setViewVisibility(R.id.media_titles, View.VISIBLE)
            view.setTextViewText(R.id.title, song.title)
            view.setTextViewText(R.id.text, getSongArtistAndAlbum(song))
        }
    }

    override fun startUpdateCover(
        context: Context,
        appWidgetIds: IntArray?,
        view: RemoteViews,
        song: Song,
        isPlaying: Boolean,
    ) {
        if (imageSize == 0) imageSize = context.resources.getDimensionPixelSize(
            R.dimen.app_widget_card_image_size
        )
        if (cardRadius == 0f) cardRadius = context.resources.getDimension(
            R.dimen.app_widget_card_radius
        )
        val fallbackColor = context.secondaryTextColor(false)

        // Load the album cover async and push the update on completion
        loadImage(
            context = context.applicationContext,
            song = song,
            widgetImageSize = imageSize,
            target = PaletteTargetBuilder()
                .defaultColor(fallbackColor)
                .onStart {
                    view.setImageViewResource(R.id.image, R.drawable.default_album_art)
                }
                .onResourceReady { result, paletteColor ->
                    val bitmap = result.toBitmapOrNull()
                    cachedCover = bitmap
                    updateWidget(view, context, isPlaying, bitmap, paletteColor)
                    pushUpdate(context, appWidgetIds, view)
                }
                .onFail {
                    updateWidget(view, context, isPlaying, null, fallbackColor)
                    pushUpdate(context, appWidgetIds, view)
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

        updateImage(context, appWidgetView, bitmap)
    }

    override fun updateImage(
        context: Context,
        view: RemoteViews,
        bitmap: Bitmap?,
    ) = view.setImageViewBitmap(
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

    override val clickableAreas: IntArray = intArrayOf(R.id.image, R.id.media_titles)

    companion object {
        const val NAME = "app_widget_card"
        private var mInstance: AppWidgetCard? = null
        private var imageSize = 0
        private var cardRadius = 0f


        @get:Synchronized
        val instance: AppWidgetCard
            get() {
                if (mInstance == null) {
                    mInstance = AppWidgetCard()
                }
                return mInstance!!
            }
    }
}
