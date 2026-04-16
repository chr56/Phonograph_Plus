package player.phonograph.ui.appwidgets

import player.phonograph.R
import player.phonograph.coil.palette.PaletteColorTarget
import player.phonograph.model.Song
import player.phonograph.util.theme.secondaryTextColor
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews

class AppWidgetClassic : BaseAppWidget() {

    override val layoutId: Int get() = R.layout.app_widget_classic

    override val name: String = NAME


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
            R.dimen.app_widget_classic_image_size
        )
        if (cardRadius == 0f) cardRadius = context.resources.getDimension(
            R.dimen.app_widget_card_radius
        )
        val fallbackColor = secondaryTextColor(context, darkBackground(context.resources))
        // Load the album cover async and push the update on completion
        loadImage(
            context = context.applicationContext,
            song = song,
            widgetImageSize = imageSize,
            target = PaletteColorTarget(
                defaultColor = fallbackColor,
                start = { _, _ ->
                    view.setImageViewResource(R.id.image, R.drawable.default_album_art)
                },
                success = { result, paletteColor ->
                    val bitmap = result.toBitmapOrNull()
                    cachedCover = bitmap
                    updateButtons(context, view, isPlaying, paletteColor)
                    updateImage(context, view, limitBitmap(bitmap))
                    pushUpdate(context, appWidgetIds, view)
                },
                error = { _, _ ->
                    updateButtons(context, view, isPlaying, fallbackColor)
                    updateImage(context, view, null)
                    pushUpdate(context, appWidgetIds, view)
                },
            )
        )
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
        const val NAME = "app_widget_classic"
        private var mInstance: AppWidgetClassic? = null
        private var imageSize = 0
        private var cardRadius = 0f

        @JvmStatic
        @get:Synchronized
        val instance: AppWidgetClassic
            get() {
                if (mInstance == null) {
                    mInstance = AppWidgetClassic()
                }
                return mInstance!!
            }
    }
}
