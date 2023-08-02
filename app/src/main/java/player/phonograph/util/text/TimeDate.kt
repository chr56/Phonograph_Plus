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

private fun lazyFormatter(pattern: String, locale: Locale = Locale.getDefault()) =
    lazy(LazyThreadSafetyMode.NONE) { SimpleDateFormat(pattern, locale) }

fun date(stamp: Long) = Date(stamp * 1000)

private val formatterDate by lazyFormatter("yyyy.MM.dd")
private val formatterTime by lazyFormatter("HH:mm:ss")
private val formatterDateShort by lazyFormatter("yy.MM.dd")
private val formatterDateTime by lazyFormatter("yyyy.MM.dd HH:mm")
private val formatterFileSuffix by lazyFormatter("_yy-MM-dd_HH-mm", Locale.US)
private val formatterFileSuffixCompat by lazyFormatter("yyMMdd_HHmmss", Locale.US)

fun dateText(timestamp: Long): String = formatterDate.format(timestamp * 1000)
fun timeText(timestamp: Long): String = formatterTime.format(timestamp * 1000)

fun shortDateText(timestamp: Long): String = formatterDateShort.format(timestamp * 1000)
fun fullDateText(timestamp: Long): String = formatterDateTime.format(timestamp * 1000)

fun datetimeSuffix(date: Date): String = formatterFileSuffix.format(date)
fun withDatetimeSuffix(string: String, date: Date): String = string + datetimeSuffix(date)
fun datetimeSuffixCompat(timestamp: Long): CharSequence = formatterFileSuffixCompat.format(timestamp)

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