/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.glide.util

import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import java.io.*
import java.util.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.model.Artist
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.ImageUtil.resizeBitmap

object CustomArtistImageUtilKt {

    fun saveCustomArtistImage(context: Context, artist: Artist, bitmap: Bitmap) {
        CoroutineScope(exceptionHandler).launch(Dispatchers.IO) {

            val dir = File(App.instance.filesDir, FOLDER_NAME).also { if (!it.exists()) it.mkdirs() }
            val file = File(dir, getArtistFileName(artist))

            val succesful: Boolean =
                try {
                    BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                        resizeBitmap(bitmap, 2048).compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                } catch (e: IOException) {
                    ErrorNotification.postErrorNotification(e, "fail to compress image size")
                    false
                }

            if (succesful) {
                val mPreferences = App.instance.getSharedPreferences(CUSTOM_ARTIST_IMAGE_PREFS, Context.MODE_PRIVATE)
                mPreferences.edit().putBoolean(getArtistFileName(artist), true).apply()
                ArtistSignatureUtil.getInstance(context).updateArtistSignature(artist.name)
                context.contentResolver.notifyChange(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null) // trigger media store changed to force artist image reload
            }
        }
    }

    fun resetCustomArtistImage(artist: Artist) {
        CoroutineScope(exceptionHandler).launch(Dispatchers.IO) {
            val mPreferences = App.instance.getSharedPreferences(CUSTOM_ARTIST_IMAGE_PREFS, Context.MODE_PRIVATE)
            mPreferences.edit().putBoolean(getArtistFileName(artist), false).apply()
            ArtistSignatureUtil.getInstance(App.instance).updateArtistSignature(artist.name)
            App.instance.contentResolver.notifyChange(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null
            ) // trigger media store changed to force artist image reload

            CustomArtistImageUtil.getFile(artist).also {
                if (it.exists()) it.delete()
            }
        }
    }
    private const val FOLDER_NAME = "/custom_artist_images/"
    private const val CUSTOM_ARTIST_IMAGE_PREFS = "custom_artist_image"

    // replace everything that is not a letter or a number with _
    fun getArtistFileName(artist: Artist): String =
        String.format(Locale.getDefault(), "#%d#%s.jpeg", artist.id, artist.name.replace("[^a-zA-Z0-9]".toRegex(), "_"))

    val exceptionHandler: CoroutineExceptionHandler get() = CoroutineExceptionHandler { _, throwable ->
        Log.w("CustomArtistImageUtilKt", throwable)
        ErrorNotification.postErrorNotification(throwable, "Fail to operate with artist image")
    }
}
