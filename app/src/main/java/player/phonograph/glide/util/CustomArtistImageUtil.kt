/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package player.phonograph.glide.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import java.io.File
import player.phonograph.App
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.util.CustomArtistImageUtilKt.getArtistFileName
import player.phonograph.glide.util.CustomArtistImageUtilKt.saveCustomArtistImage
import player.phonograph.model.Artist
import player.phonograph.notification.ErrorNotification.postErrorNotification

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class CustomArtistImageUtil constructor(val context: Context) {
    private val mPreferences: SharedPreferences = context.applicationContext.getSharedPreferences(CUSTOM_ARTIST_IMAGE_PREFS, Context.MODE_PRIVATE)
    fun setCustomArtistImage(artist: Artist, uri: Uri) {
        Glide.with(context)
            .asBitmap()
            .apply(SongGlideRequest.DEFAULT_OPTION)
            .skipMemoryCache(true)
            .load(uri)
            .listener(
                object : RequestListener<Bitmap?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Bitmap?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.w(TAG, "Fail to load artist cover:")
                        Log.i(TAG, "   Artist" + artist.name + " " + artist.id)
                        Log.i(TAG, "   Uri:  $uri")
                        postErrorNotification(e!!, "Fail to save custom artist image")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any,
                        target: Target<Bitmap?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        saveCustomArtistImage(context, artist, resource!!)
                        return false
                    }
                }
            )
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {}
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {}
            })
    }

    // shared prefs saves us many IO operations
    fun hasCustomArtistImage(artist: Artist?): Boolean {
        return mPreferences.getBoolean(getArtistFileName(artist!!), false)
    }

    companion object {
        private const val TAG = "ArtistCoverImage"

        private const val CUSTOM_ARTIST_IMAGE_PREFS = "custom_artist_image"
        private const val FOLDER_NAME = "/custom_artist_images/"
        private var sInstance: CustomArtistImageUtil? = null
        @JvmStatic
        fun getInstance(context: Context): CustomArtistImageUtil? {
            if (sInstance == null) {
                sInstance = CustomArtistImageUtil(context.applicationContext)
            }
            return sInstance
        }

        @JvmStatic
        fun getFile(artist: Artist?): File {
            val dir = File(App.instance.filesDir, FOLDER_NAME)
            return File(dir, getArtistFileName(artist!!))
        }
    }
}
