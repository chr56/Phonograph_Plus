/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph

import android.util.Log

/**
 * only run [block] on [BuildConfig.DEBUG] build
 */
inline fun debug(crossinline block: () -> Unit) {
    if (BuildConfig.DEBUG) block()
}


fun logMetrics(message: String) {
    Log.v("Metrics", "[${System.currentTimeMillis().mod(100000)}] $message")
}