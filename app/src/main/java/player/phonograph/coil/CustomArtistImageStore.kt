/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import coil.Coil
import coil.request.ImageRequest
import coil.target.Target
import player.phonograph.util.reportError
import player.phonograph.util.ui.BitmapUtil.restraintBitmapSize
import player.phonograph.util.warning
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Class that manage custom artist image
 * @author Karim Abou Zeid (kabouzeid), chr_56
 */
class CustomArtistImageStore private constructor(context: Context) {

    companion object {
        private const val TAG = "CustomArtistImageStore"
        private const val SUB_FOLDER_NAME: String = "/custom_artist_images/"
        private const val CUSTOM_ARTIST_IMAGE_PREFS: String = "custom_artist_image"

        private var sInstance: CustomArtistImageStore? = null
        fun instance(context: Context): CustomArtistImageStore {
            return sInstance ?: CustomArtistImageStore(context).apply { sInstance = this }
        }
    }

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            CUSTOM_ARTIST_IMAGE_PREFS,
            Context.MODE_PRIVATE
        )

    private val storeDir: File = File(context.filesDir, SUB_FOLDER_NAME)

    /**
     * @return the unique file name of a artist
     */
    fun getArtistFileName(artistId: Long, artistName: String): String {
        val artistNameSafe = artistName.replace(Regex("[^a-zA-Z0-9]"), "_")
        return "#$artistId#$artistNameSafe.jpeg"
    }

    /**
     * @return the custom ArtistImage file, null if not set
     */
    fun getCustomArtistImageFile(artistId: Long, artistName: String): File? {
        val exist = preferences.getBoolean(getArtistFileName(artistId, artistName), false)
        return if (exist) {
            File(storeDir, getArtistFileName(artistId, artistName))
        } else {
            null
        }
    }

    /**
     * set a custom artist image
     */
    fun setCustomArtistImage(context: Context, artistId: Long, artistName: String, source: Uri) {
        Coil.imageLoader(context).enqueue(
            ImageRequest
                .Builder(context)
                .data(source)
                .target(
                    object : Target {
                        private val id = artistId
                        private val name = artistName

                        override fun onError(error: Drawable?) {
                            warning(TAG, "Failed to load this image $source for $artistName")
                        }

                        override fun onSuccess(result: Drawable) {
                            val bitmap = result.toBitmap()
                            setCustomArtistImage(context, id, name, bitmap)
                        }
                    }
                )
                .build()
        )
    }

    /**
     * set a custom artist image
     */
    fun setCustomArtistImage(context: Context, artistId: Long, artistName: String, bitmap: Bitmap) {
        CoroutineScope(SupervisorJob())
            .launch(Dispatchers.IO) {
                val file = File(storeDir, getArtistFileName(artistId, artistName))
                val success = try {
                    BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                        bitmap.restraintBitmapSize(2048, true)
                            .compress(JPEG, 100, outputStream)
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    reportError(e, TAG, "Can not save custom image for $artistName")
                    false
                }
                if (success) {
                    try {
                        preferences.edit()
                            .putBoolean(getArtistFileName(artistId, artistName), true)
                            .apply()
                        context.contentResolver.notifyChange(EXTERNAL_CONTENT_URI, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        reportError(e, TAG, "Failed to update custom image $artistName")
                    }
                }
            }
    }

    /**
     * remove a custom artist image if exist
     */
    fun resetCustomArtistImage(context: Context, artistId: Long, artistName: String) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            try {
                File(storeDir, getArtistFileName(artistId, artistName)).delete()
                preferences.edit().putBoolean(getArtistFileName(artistId, artistName), false).apply()
                context.contentResolver.notifyChange(EXTERNAL_CONTENT_URI, null)
                // trigger media store changed to force artist image reload
            } catch (e: Exception) {
                e.printStackTrace()
                reportError(e, TAG, "Failed to reset xustom artist image of$artistName")
            }
        }
    }
}
