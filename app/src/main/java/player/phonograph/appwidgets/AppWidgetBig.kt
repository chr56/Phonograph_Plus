package player.phonograph.appwidgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import player.phonograph.R
import player.phonograph.appwidgets.base.BaseAppWidget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.service.MusicService
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.ImageUtil
import player.phonograph.util.Util.getScreenSize
import util.mddesign.util.MaterialColorHelper

class AppWidgetBig : BaseAppWidget() {
    private var target: Target<Bitmap>? = null // for cancellation

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(context.packageName, R.layout.app_widget_big)

        setupDefaultPhonographWidgetAppearance(context, appWidgetView)
        setupDefaultPhonographWidgetButtons(context, appWidgetView)

        setupAdditionalWidgetAppearance(context, appWidgetView)
        setupAdditionalWidgetButtons(context, appWidgetView)

        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(service.packageName, R.layout.app_widget_big)
        val isPlaying = service.isPlaying
        val song = service.currentSong

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
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service, playPauseRes, MaterialColorHelper.getPrimaryTextColor(service, false)
                )
            )
        )

        // Set prev/next button drawables
        appWidgetView.setImageViewBitmap(
            R.id.button_next,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service, R.drawable.ic_skip_next_white_24dp, MaterialColorHelper.getPrimaryTextColor(service, false)
                )
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service, R.drawable.ic_skip_previous_white_24dp, MaterialColorHelper.getPrimaryTextColor(service, false)
                )
            )
        )

        // Link actions buttons to intents
        setupDefaultPhonographWidgetButtons(service, appWidgetView)
        setupAdditionalWidgetButtons(service, appWidgetView)

        // Load the album cover async and push the update on completion
        val p = getScreenSize(service)
        val widgetImageSize = p.x.coerceAtMost(p.y)
        val appContext = service.applicationContext
        service.runOnUiThread {
            if (target != null) {
                Glide.with(service).clear(target)
            }
            target =
                SongGlideRequest.Builder
                .from(Glide.with(appContext), song)
                .checkIgnoreMediaStore(appContext)
                .asBitmap().build()
                .into(object : SimpleTarget<Bitmap?>(widgetImageSize, widgetImageSize) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        update(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        update(null)
                    }

                    private fun update(bitmap: Bitmap?) {
                        if (bitmap == null) {
                            appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art)
                        } else {
                            appWidgetView.setImageViewBitmap(R.id.image, bitmap)
                        }
                        pushUpdate(appContext, appWidgetIds, appWidgetView)
                    }
                }) as Target<Bitmap>?
        }
    }

    override fun setupAdditionalWidgetButtons(context: Context, view: RemoteViews) {
        // Home
        val action = Intent(context, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_IMMUTABLE)
        view.setOnClickPendingIntent(R.id.clickable_area, pendingIntent)
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
