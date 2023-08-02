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


fun datetimeSuffix(date: Date): CharSequence = SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.US).format(date)
fun withDatetimeSuffix(string: String, date: Date): String = string + datetimeSuffix(date)

/**
 * convert a timestamp to a readable String
 *
 * @param t timeStamp in milliseconds to parse
 * @return `%d:%02d.%03d` partner time string
 */
fun parseTimeStamp(t: Int): String {
    val ms = (t % 1000).toLong()
    val s = (t % (1000 * 60) / 1000).toLong()
    val m = (t - s * 1000 - ms) / (1000 * 60)
    return String.format("%d:%02d.%03d", m, s, ms)
}