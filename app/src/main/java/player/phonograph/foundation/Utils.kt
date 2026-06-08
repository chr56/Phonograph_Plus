/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation

import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.Context
import java.text.Normalizer


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
// Sort
//

private val combiningMarksRegex = "\\p{Mn}+".toRegex()

fun String.normalizeForSort(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(combiningMarksRegex, "")
        .lowercase()

inline fun <T> List<T>.sort(
    revert: Boolean,
    crossinline selector: (T) -> Comparable<*>?,
): List<T> {
    return if (revert) this.sortedWith(compareByDescending(selector))
    else this.sortedWith(compareBy(selector))
}



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
// Bit Mask
//

fun Int.testBit(mask: Int): Boolean = (this and mask) != 0
fun Int.setBit(mask: Int): Int = (this or mask)
fun Int.unsetBit(mask: Int): Int = (this and mask.inv())
