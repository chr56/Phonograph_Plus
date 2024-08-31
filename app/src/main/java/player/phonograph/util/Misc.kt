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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.util.Log
import android.widget.Toast
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream


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
// Context check
//

inline fun activity(context: Context, block: (Activity) -> Boolean): Boolean =
    if (context is Activity) {
        block(context)
    } else {
        false
    }

inline fun fragmentActivity(context: Context, block: (FragmentActivity) -> Boolean): Boolean =
    if (context is FragmentActivity) {
        block(context)
    } else {
        false
    }

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
 * run [block] in main thread via Handler
 */
inline fun runOnMainHandler(crossinline block: () -> Unit) =
    Handler(Looper.getMainLooper()).post { block() }


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


/**
 * wrapped `withTimeout` to support negative timeMillis
 * @param timeMillis negative if no timeout
 * @param context the [CoroutineContext] for [block] if no timeout
 * @see withTimeout
 */
suspend fun <T> withTimeoutOrNot(
    timeMillis: Long,
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = if (timeMillis > 0) withTimeout(timeMillis, block) else block(CoroutineScope(context))

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
// Metrics

fun logMetrics(stage: String) {
    Log.v(
        "Metrics",
        "[${System.currentTimeMillis().mod(100000)}] $stage"
    )
}

//
// Other
//

val primaryExternalStoragePath: String = Environment.getExternalStorageDirectory().absolutePath

private val albumArtContentUri: Uri by lazy(LazyThreadSafetyMode.NONE) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.AUTHORITY_URI.buildUpon()
            .appendPath(MediaStore.VOLUME_EXTERNAL)
            .appendPath("audio")
            .appendPath("albumart")
            .build()
    else Uri.parse("content://media/external/audio/albumart")
}

fun mediaStoreAlbumArtUri(albumId: Long): Uri = ContentUris.withAppendedId(albumArtContentUri, albumId)

fun mediaStoreSongUri(songId: Long): Uri = ContentUris.withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI, songId)


fun openOutputStreamSafe(context: Context, uri: Uri, mode: String): OutputStream? =
    try {
        @SuppressLint("Recycle")
        val outputStream = context.contentResolver.openOutputStream(uri, mode)
        if (outputStream == null) warning("UriUtil", "Failed to open ${uri.path}")
        outputStream
    } catch (e: FileNotFoundException) {
        reportError(e, "UriUtil", "File Not found (${uri.path})")
        null
    }

internal const val PLAYLIST_MIME_TYPE = "audio/x-mpegurl"

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

private const val BITWISE_POSITION_MASK: Long = 0x00ff_fff0_0000_0000
private const val BITWISE_POSITION_CUT_MASK: Long = 0xf_ffff // 20 bits
private const val BITWISE_SHIFT: Int = 36 // 4*9

/**
 * Generate a new ID associated with a position, making it safe to used in some lists allowing duplicated item
 * @param id original id
 * @param position related position
 * @return new id which is safe to used in a list allowing duplicated item
 */
fun produceSafeId(id: Long, position: Int): Long {
    val cleared: Long = id and BITWISE_POSITION_MASK.inv()
    val shifted: Long = (position.toLong() and BITWISE_POSITION_CUT_MASK) shl BITWISE_SHIFT
    return cleared or shifted
}


private const val MASK_SHIFT = Long.SIZE_BITS - Byte.SIZE_BITS
private const val MASK_LOWER_56BITS = (1L shl MASK_SHIFT) - 1 // 0x00ff_ffff_ffff_ffff
/**
 * Generate a sectioned 64-bits ID: the lowers 56 bits is cut from [id], the higher 8 bits is shifted from [section]
 */
fun produceSectionedId(id: Long, section: Int): Long {
    return (id and MASK_LOWER_56BITS) + (section shl MASK_SHIFT)
}

