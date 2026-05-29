/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.text

fun String?.bracketedIfAny(): String {
    return if (!isNullOrEmpty()) "($this)" else ""
}