/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH)
fun dateString(stamp: Long): String {
    return dateFormatter.format(Date(stamp * 1000))
}