/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package player.phonograph.glide.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.glide.SongGlideRequest
import player.phonograph.model.Artist
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.ImageUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object CustomArtistImageUtil {
    private val preferences: SharedPreferences by lazy {
        App.instance.applicationContext.getSharedPreferences(CUSTOM_ARTIST_IMAGE_PREFS, Context.MODE_PRIVATE)
    }

    fun setCustomArtistImage(artist: Artist, uri: Uri) {
        Glide.with(App.instance)
            .asBitmap()
            .apply(SongGlideRequest.DEFAULT_OPTION)
            .skipMemoryCache(true)
            .load(uri)
            .listener(object : RequestListener<Bitmap?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Bitmap?>,
                    isFirstResource: Boolean,
                ): Boolean {
                    Log.w(TAG, "Fail to load artist cover:")
                    ErrorNotification.postErrorNotification(
                        e!!, "Fail to save custom artist image\n   Artist${artist.name} ${artist.id}   Uri:  $uri"
                    )
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any,
                    target: Target<Bitmap?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    saveCustomArtistImage(App.instance, artist, resource!!)
                    return false
                }
            })
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {}
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {}
            })
    }

    // shared prefs saves us many IO operations
    fun hasCustomArtistImage(artist: Artist?): Boolean {
        return preferences.getBoolean(getArtistFileName(artist!!), false)
    }

    fun saveCustomArtistImage(context: Context, artist: Artist, bitmap: Bitmap) {
        CoroutineScope(exceptionHandler).launch(Dispatchers.IO) {

            val dir = File(App.instance.filesDir, FOLDER_NAME).also { if (!it.exists()) it.mkdirs() }
            val file = File(dir, getArtistFileName(artist))

            val successful: Boolean =
                try {
                    BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                        ImageUtil.resizeBitmap(bitmap, 2048).compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                } catch (e: IOException) {
                    ErrorNotification.postErrorNotification(e, "fail to compress image size")
                    false
                }

            if (successful) {
                preferences.edit().putBoolean(getArtistFileName(artist), true).apply()
                ArtistSignatureUtil.getInstance(context).updateArtistSignature(artist.name)
                context.contentResolver.notifyChange(
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    null
                ) // trigger media store changed to force artist image reload
            }
        }
    }

    fun resetCustomArtistImage(artist: Artist) {
        CoroutineScope(exceptionHandler).launch(Dispatchers.IO) {
            preferences.edit().putBoolean(getArtistFileName(artist), false).apply()
            ArtistSignatureUtil.getInstance(App.instance).updateArtistSignature(artist.name)
            App.instance.contentResolver.notifyChange(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null
            ) // trigger media store changed to force artist image reload

            getFile(artist).also {
                if (it.exists()) it.delete()
            }
        }
    }

    private const val FOLDER_NAME: String = "/custom_artist_images/"
    private const val CUSTOM_ARTIST_IMAGE_PREFS: String = "custom_artist_image"

    private const val TAG = "ArtistCoverImage"

    fun getFile(artist: Artist): File {
        val dir = File(App.instance.filesDir, FOLDER_NAME)
        return File(dir, getArtistFileName(artist))
    }

    // replace everything that is not a letter or a number with _
    fun getArtistFileName(artist: Artist): String =
        String.format(Locale.getDefault(), "#%d#%s.jpeg", artist.id, artist.name.replace("[^a-zA-Z0-9]".toRegex(), "_"))

    private val exceptionHandler: CoroutineExceptionHandler
        get() = CoroutineExceptionHandler { _, throwable ->
            Log.w(TAG, throwable)
            ErrorNotification.postErrorNotification(throwable, "Fail to operate with artist image")
        }
}
