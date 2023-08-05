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
import player.phonograph.notification.ErrorNotification
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
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
// Exception
//

fun reportError(e: Throwable, tag: String, message: String) {
    Log.e(tag, message, e)
    ErrorNotification.postErrorNotification(e, message)
}

fun warning(tag: String, message: String) {
    Log.w(tag, message)
    ErrorNotification.postErrorNotification(message)
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
// Looper
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

//
// Coroutine
//

fun createDefaultExceptionHandler(@Suppress("UNUSED_PARAMETER") TAG: String, defaultMessageHeader: String = "Error!"): CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, exception ->
        ErrorNotification.postErrorNotification(
            exception,
            "$defaultMessageHeader:${exception.message}"
        )
    }

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
// Other
//

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

//
// Parcelable
//

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    when {
        SDK_INT >= TIRAMISU -> getParcelable(key, T::class.java)
        else                -> getParcelable(key) as? T
    }

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? =
    when {
        SDK_INT >= TIRAMISU -> getParcelableArrayList(key, T::class.java)
        else                -> getParcelableArrayList(key)
    }


@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? =
    when {
        SDK_INT >= TIRAMISU -> getParcelableExtra(key, T::class.java)
        else                -> getParcelableExtra(key) as? T
    }

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.parcelableArrayListExtra(key: String): ArrayList<T>? =
    when {
        SDK_INT >= TIRAMISU -> getParcelableArrayListExtra(key, T::class.java)
        else                -> getParcelableArrayListExtra(key)
    }


@Suppress("DEPRECATION", "UNCHECKED_CAST")
inline fun <reified T> Parcel.array(classLoader: ClassLoader?): Array<T>? =
    when {
        SDK_INT >= TIRAMISU -> readArray(classLoader, T::class.java)
        else                -> readArray(classLoader) as? Array<T>
    }

@Suppress("DEPRECATION")
inline fun <reified T> Parcel.sparseArray(classLoader: ClassLoader?): SparseArray<T>? =
    when {
        SDK_INT >= TIRAMISU -> readSparseArray(classLoader, T::class.java)
        else                -> readSparseArray(classLoader)
    }