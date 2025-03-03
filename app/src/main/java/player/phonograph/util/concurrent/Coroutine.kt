/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.concurrent

import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.widget.Toast
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout


/**
 * try to get [Context]'s LifecycleScope or create a new one with [coroutineContext]
 */
fun Context.lifecycleScopeOrNewOne(coroutineContext: CoroutineContext = SupervisorJob()) =
    (this as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(coroutineContext)



//region Toast
suspend fun coroutineToast(context: Context, text: String, longToast: Boolean = false) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, text, if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}

suspend fun coroutineToast(context: Context, @StringRes res: Int, longToast: Boolean = false) =
    coroutineToast(context, context.getString(res), longToast)
//endregion

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