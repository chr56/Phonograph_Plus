/*
 *  Copyright (c) 2022~2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */

package player.phonograph.util

import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.model.Song
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.widget.Toast
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File


//
// debug
//
/**
 * only run [block] on [DEBUG] build
 */
inline fun debug(crossinline block: () -> Unit) {
    if (DEBUG) block()
}

//
// Bit Mask
//

fun Int.testBit(mask: Int): Boolean = (this and mask) != 0
fun Int.setBit(mask: Int): Int = (this or mask)
fun Int.unsetBit(mask: Int): Int = (this and mask.inv())

//
// LocalBoardCast
//

fun sentPlaylistChangedLocalBoardCast() =
    LocalBroadcastManager.getInstance(App.instance).sendBroadcast(
        Intent(BROADCAST_PLAYLISTS_CHANGED)
    )

//
// Looper & Handler
//

/**
 * wrap with looper check
 */
inline fun withLooper(crossinline block: () -> Unit) {
    if (Looper.myLooper() == null) {
        Looper.prepare()
        block()
        Looper.loop()
    } else {
        block()
    }
}

/**
 * post a delayed message with callback which can only be called for _ONCE_ (without dither due to multiple call in a short time)
 * @param handler target handler
 * @param id `what` of the message
 * @return true if the message was successfully placed in to the message queue
 */
fun postDelayedOnceHandlerCallback(
    handler: Handler,
    delay: Long,
    id: Int = delay.toInt(),
    callback: Runnable,
): Boolean {
    val message = Message.obtain(handler, callback).apply { what = id }
    handler.removeMessages(id)
    return handler.sendMessageDelayed(message, delay)
}

//
// Coroutine
//

suspend fun coroutineToast(context: Context, text: String, longToast: Boolean = false) {
    withContext(Dispatchers.Main) {
        Toast.makeText(
            context,
            text,
            if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }
}

suspend fun coroutineToast(context: Context, @StringRes res: Int) =
    coroutineToast(context, context.getString(res))

/**
 * try to get [Context]'s LifecycleScope or create a new one with [coroutineContext]
 */
fun Context.lifecycleScopeOrNewOne(coroutineContext: CoroutineContext = SupervisorJob()) =
    (this as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(coroutineContext)

//
// Reflection
//

@Throws(NoSuchFieldException::class, SecurityException::class)
inline fun <reified T, reified F> T.reflectDeclaredField(fieldName: String): F {
    val f =
        T::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
        }
    return f.get(this) as F
}

//
// Sort
//

inline fun <T> List<T>.sort(
    revert: Boolean,
    crossinline selector: (T) -> Comparable<*>?,
): List<T> {
    return if (revert) this.sortedWith(compareByDescending(selector))
    else this.sortedWith(compareBy(selector))
}

//
// Other
//

fun mediaStoreAlbumArtUri(albumId: Long): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val uri = MediaStore.AUTHORITY_URI.buildUpon()
            .appendPath(MediaStore.VOLUME_EXTERNAL)
            .appendPath("audio")
            .appendPath("albumart")
            .build()
        ContentUris.withAppendedId(uri, albumId)
    } else {
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    }

fun shareFileIntent(context: Context, song: Song): Intent {
    return try {
        Intent()
            .setAction(Intent.ACTION_SEND)
            .putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(context, context.applicationContext.packageName, File(song.data))
            )
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setType("audio/*")
    } catch (e: IllegalArgumentException) {
        // the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
        reportError(e, "Share", "Physical external SD card is not fully support!")
        Intent()
    }
}