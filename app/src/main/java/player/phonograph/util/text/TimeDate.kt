/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.text

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun currentDate(): Date = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).time
fun currentTimestamp(): Long = currentDate().time

fun currentDateTime(): CharSequence = DateFormat.format("yyMMdd_HHmmss", currentDate())

fun date(stamp: Long) = Date(stamp * 1000)
fun dateText(stamp: Long) = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(date(stamp))
fun timeText(stamp: Long) = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date(stamp))
