/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.metadata

fun interface ExceptionCollector {
    fun collect(exception: Throwable)
}