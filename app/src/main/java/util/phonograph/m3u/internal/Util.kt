/*
 * Copyright (c) 2022 chr_56
 */

package util.phonograph.m3u.internal

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun appendTimestampSuffix(string: String): String {
    val suffix = SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.getDefault()).format(Calendar.getInstance().time)
    return string + suffix
}
