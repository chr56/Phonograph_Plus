/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.text

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun currentDate(): Date = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).time
fun currentTimestamp(): Long = currentDate().time

private fun lazyFormatter(pattern: String, locale: Locale = Locale.getDefault()) =
    lazy(LazyThreadSafetyMode.NONE) { SimpleDateFormat(pattern, locale) }

private val formatterDate by lazyFormatter("yyyy.MM.dd")
private val formatterTime by lazyFormatter("HH:mm:ss")

private val formatterDateShort by lazyFormatter("yy.MM.dd")
private val formatterDateTime by lazyFormatter("yyyy.MM.dd HH:mm")

private val formatterFileSuffix by lazyFormatter("_yy-MM-dd_HH-mm", Locale.US)
private val formatterFileSuffixCompat by lazyFormatter("yyMMdd_HHmmss", Locale.US)

private val formatterLyrics by lazyFormatter("mm:ss.SSS")

fun dateText(timestampInSec: Long): String = formatterDate.format(timestampInSec * 1000)
fun timeText(timestampInSec: Long): String = formatterTime.format(timestampInSec * 1000)
fun dateTimeText(timestampInSec: Long): String = formatterDateTime.format(timestampInSec * 1000)
fun dateTextShortText(timestampInSec: Long): String = formatterDateShort.format(timestampInSec * 1000)
fun dateTimeSuffix(date: Date): String = formatterFileSuffix.format(date)
fun dateTimeSuffixCompat(date: Date): CharSequence = formatterFileSuffixCompat.format(date)

fun lyricsTimestamp(timeStamp: Int): String = formatterLyrics.format(timeStamp)