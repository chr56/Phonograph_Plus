/*
 *  Copyright (c) 2022~2023 chr_56
 */

/**
 * @author Eugene Cheung (arkon), chr_56
 */
package player.phonograph.util.time

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone


private val calendar: Calendar by lazy { Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()) }


fun past(duration: Duration): Long {
    return duration.toSeconds()
}

fun recently(duration: Duration): Long {
    return when (duration) {
        is Duration.Second -> duration.value
        is Duration.Minute -> elapsedMinute(duration.value)
        is Duration.Hour   -> elapsedHour(duration.value)
        is Duration.Day    -> elapsedDay(duration.value)
        is Duration.Week   -> elapsedWeek(duration.value)
        is Duration.Month  -> elapsedMonth(duration.value)
        is Duration.Year   -> elapsedYear(duration.value)
    }
}

private fun elapsedMinute(minutes: Long): Long =
    calendar[Calendar.SECOND] + (minutes - 1) * TimeUnit.SECONDS_PER_MINUTE

private fun elapsedHour(hours: Long): Long =
    calendar[Calendar.MINUTE] * TimeUnit.SECONDS_PER_MINUTE + (hours - 1) * TimeUnit.SECONDS_PER_HOUR


private val elapsedToday
    get() = (calendar[Calendar.HOUR_OF_DAY] * 60 + calendar[Calendar.MINUTE]).toLong() * TimeUnit.SECONDS_PER_MINUTE +
            calendar[Calendar.SECOND]

private fun elapsedDay(days: Long): Long {
    val restDays = days - 1
    return if (restDays > 0) {
        restDays * TimeUnit.SECONDS_PER_DAY + elapsedToday
    } else {
        elapsedToday
    }
}

private fun elapsedWeek(weeks: Long): Long {
    val restDays = (weeks - 1) * TimeUnit.DAYS_PER_WEEK + (calendar[Calendar.DAY_OF_WEEK] - 1)
    return if (restDays > 0) {
        restDays * TimeUnit.SECONDS_PER_DAY + elapsedToday
    } else {
        elapsedToday
    }
}

private fun elapsedMonth(months: Long): Long {
    val restDaysThisMonth = calendar[Calendar.DAY_OF_MONTH] - 1
    var elapsed = restDaysThisMonth * TimeUnit.SECONDS_PER_DAY + elapsedToday

    var month = calendar[Calendar.MONTH]
    var year = calendar[Calendar.YEAR]
    for (i in 0 until months - 1) {
        month--
        if (month < Calendar.JANUARY) {
            month = Calendar.DECEMBER
            year--
        }
        elapsed += getDaysInMonth(year, month) * TimeUnit.SECONDS_PER_DAY
    }
    return elapsed

}

private fun elapsedYear(years: Long): Long {
    var elapsed = elapsedMonth(calendar[Calendar.MONTH].toLong() + 1)

    var year = calendar[Calendar.YEAR]
    for (i in 0 until years - 1) {
        year--
        elapsed += getDaysInYear(year) * TimeUnit.SECONDS_PER_DAY
    }
    return elapsed

}


/**
 * Gets the number of days for the given month in the given year.
 *
 * @param year  The year.
 * @param month The month (1 - 12).
 * @return The days in that month/year.
 */
private fun getDaysInMonth(year: Int, month: Int): Int =
    GregorianCalendar(year, month, 1).getActualMaximum(Calendar.DAY_OF_MONTH)

/**
 * Gets the number of days for the given year.
 *
 * @param year The year
 * @return The days in that year.
 */
private fun getDaysInYear(year: Int): Int =
    GregorianCalendar(year, 1, 1).getActualMaximum(Calendar.DAY_OF_YEAR)

