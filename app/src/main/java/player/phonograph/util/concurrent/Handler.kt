/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.concurrent

import android.os.Handler
import android.os.Looper
import android.os.Message

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