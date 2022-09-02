package player.phonograph.appwidgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.Disposable
import coil.request.ImageRequest
import kotlinx.coroutines.Deferred
import player.phonograph.App
import player.phonograph.R
import player.phonograph.appwidgets.Util.createRoundedBitmap
import player.phonograph.appwidgets.base.BaseAppWidget
import player.phonograph.coil.target.ColoredTarget
import player.phonograph.service.MusicService
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.ImageUtil
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PaletteUtil.getColor
import util.mddesign.util.MaterialColorHelper.getSecondaryTextColor

class AppWidgetCard : BaseAppWidget() {

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(context.packageName, R.layout.app_widget_card)

        setupDefaultPhonographWidgetAppearance(context, appWidgetView)
        setupDefaultPhonographWidgetButtons(context, appWidgetView)

        setupAdditionalWidgetAppearance(context, appWidgetView)
        setupAdditionalWidgetButtons(context, appWidgetView)

        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    private var task: Disposable? = null

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(service.packageName, R.layout.app_widget_card)
        val isPlaying = service.isPlaying
        val song = App.instance.queueManager.currentSong

        // Set the titles and artwork
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE)
            appWidgetView.setTextViewText(R.id.title, song.title)
            appWidgetView.setTextViewText(R.id.text, getSongArtistAndAlbum(song))
        }

        // Set correct drawable for pause state
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service,
                    playPauseRes(isPlaying),
                    getSecondaryTextColor(service, true)
                )
            )
        )

        // Set prev/next button drawables
        appWidgetView.setImageViewBitmap(
            R.id.button_next,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service,
                    R.drawable.ic_skip_next_white_24dp,
                    getSecondaryTextColor(service, true)
                )
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service,
                    R.drawable.ic_skip_previous_white_24dp,
                    getSecondaryTextColor(service, true)
                )
            )
        )

        // Link actions buttons to intents
        setupDefaultPhonographWidgetButtons(service, appWidgetView)
        setupAdditionalWidgetButtons(service, appWidgetView)
        if (imageSize == 0) imageSize = service.resources.getDimensionPixelSize(
            R.dimen.app_widget_card_image_size
        )
        if (cardRadius == 0f) cardRadius = service.resources.getDimension(
            R.dimen.app_widget_card_radius
        )

        // Load the album cover async and push the update on completion
        uiHandler.post {
            val appContext = service.applicationContext
            val loader = Coil.imageLoader(appContext)
            task?.dispose() // cancel last
            task = loader.enqueue(
                ImageRequest.Builder(appContext)
                    .data(song)
                    .size(imageSize, imageSize)
                    .target(object : ColoredTarget() {

                        val fallbackColor: Int =
                            getSecondaryTextColor(service, true)

                        override fun onStart(placeholder: Drawable?) {
                            appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art)
                        }

                        override fun onReady(drawable: Drawable, palette: Deferred<Palette>?) {
                            palette?.getColor(fallbackColor) { color ->
                                update(drawable.toBitmapOrNull(), color)
                            }
                        }

                        override fun onError(error: Drawable?) {
                            update(null, fallbackColor)
                        }

                        fun update(bitmap: Bitmap?, color: Int) {
                            // Set correct drawable for pause state
                            appWidgetView.setImageViewBitmap(
                                R.id.button_toggle_play_pause,
                                ImageUtil.createBitmap(service.getTintedDrawable(playPauseRes(isPlaying), color)!!)
                            )

                            // Set prev/next button drawables
                            appWidgetView.setImageViewBitmap(
                                R.id.button_next,
                                ImageUtil.createBitmap(service.getTintedDrawable(R.drawable.ic_skip_next_white_24dp, color)!!)
                            )
                            appWidgetView.setImageViewBitmap(
                                R.id.button_prev,
                                ImageUtil.createBitmap(service.getTintedDrawable(R.drawable.ic_skip_previous_white_24dp, color)!!)
                            )

                            val image = getAlbumArtDrawable(service.resources, bitmap)
                            val roundedBitmap =
                                createRoundedBitmap(
                                    image,
                                    imageSize,
                                    imageSize,
                                    cardRadius,
                                    0f,
                                    cardRadius,
                                    0f
                                )
                            appWidgetView.setImageViewBitmap(R.id.image, roundedBitmap)
                            pushUpdate(service, appWidgetIds, appWidgetView)
                        }
                    })
                    .build()
            )
        }
    }

    override fun setupAdditionalWidgetButtons(context: Context, view: RemoteViews) {
        // Home
        val action = Intent(context, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            action,
            PendingIntent.FLAG_IMMUTABLE
        )
        view.setOnClickPendingIntent(R.id.image, pendingIntent)
        view.setOnClickPendingIntent(R.id.media_titles, pendingIntent)
    }

    private val uiHandler: Handler by lazy { Handler(Looper.getMainLooper()) }


    private fun playPauseRes(isPlaying: Boolean) = if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp

    companion object {
        const val NAME = "app_widget_card"
        private var mInstance: AppWidgetCard? = null
        private var imageSize = 0
        private var cardRadius = 0f

        @JvmStatic
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
