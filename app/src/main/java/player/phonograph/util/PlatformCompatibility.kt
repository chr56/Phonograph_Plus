/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util

import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RegisterReceiverFlags
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import android.util.SparseArray
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Objects


/** Storage Volume `external` **/
val MEDIASTORE_VOLUME_EXTERNAL = if (SDK_INT >= VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL else "external"

@Throws(IOException::class)
fun InputStream.transferToOutputStream(outputStream: OutputStream): Long {
    return if (SDK_INT >= TIRAMISU) {
        transferTo(outputStream)
    } else {
        Objects.requireNonNull(outputStream, "out")
        var transferred: Long = 0
        val buffer = ByteArray(8192)
        var read: Int
        while (this.read(buffer, 0, 8192).also { read = it } >= 0) {
            outputStream.write(buffer, 0, read)
            transferred += read.toLong()
        }
        transferred
    }
}

// See 242048899

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    when {
        SDK_INT >= UPSIDE_DOWN_CAKE -> getParcelable(key, T::class.java)
        else                        -> getParcelable(key) as? T
    }
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? =
    when {
        SDK_INT >= UPSIDE_DOWN_CAKE -> getParcelableArrayList(key, T::class.java)
        else                        -> getParcelableArrayList(key)
    }
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? =
    when {
        SDK_INT >= UPSIDE_DOWN_CAKE -> getParcelableExtra(key, T::class.java)
        else                        -> getParcelableExtra(key) as? T
    }
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.parcelableArrayListExtra(key: String): ArrayList<T>? =
    when {
        SDK_INT >= UPSIDE_DOWN_CAKE -> getParcelableArrayListExtra(key, T::class.java)
        else                        -> getParcelableArrayListExtra(key)
    }
@Suppress("DEPRECATION", "UNCHECKED_CAST")
inline fun <reified T> Parcel.array(classLoader: ClassLoader?): Array<T>? =
    when {
        SDK_INT >= UPSIDE_DOWN_CAKE -> readArray(classLoader, T::class.java)
        else                        -> readArray(classLoader) as? Array<T>
    }
@Suppress("DEPRECATION")
inline fun <reified T> Parcel.sparseArray(classLoader: ClassLoader?): SparseArray<T>? =
    when {
        SDK_INT >= UPSIDE_DOWN_CAKE -> readSparseArray(classLoader, T::class.java)
        else                        -> readSparseArray(classLoader)
    }

fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver?,
    filter: IntentFilter,
    @RegisterReceiverFlags flags: Int = ContextCompat.RECEIVER_NOT_EXPORTED,
) = ContextCompat.registerReceiver(this, receiver, filter, flags)

