/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

object ReflectUtil {
    @Throws(NoSuchFieldException::class, SecurityException::class)
    inline fun <reified T, reified F> T.reflectDeclaredField(fieldName: String): F {
        val f =
            T::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
            }
        return f.get(this) as F
    }
}