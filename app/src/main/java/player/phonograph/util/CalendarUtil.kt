package player.phonograph.util

import java.util.*

/**
 * @author Eugene Cheung (arkon)
 */
object CalendarUtil {

    private val calendar: Calendar by lazy { Calendar.getInstance() }

    /**
     * Returns the time elapsed so far today in milliseconds.
     *
     * @return Time elapsed today in milliseconds.
     */
    val elapsedToday: Long
        // Time elapsed so far today
        get() =
            (calendar[Calendar.HOUR_OF_DAY] * 60 + calendar[Calendar.MINUTE]) * MS_PER_MINUTE +
                calendar[Calendar.SECOND] * 1000 +
                calendar[Calendar.MILLISECOND]

    /**
     * Returns the time elapsed so far last N days in milliseconds.
     *
     * @return Time elapsed since N days in milliseconds.
     */
    fun getElapsedDays(numDays: Int): Long {
        var elapsed = elapsedToday
        elapsed += numDays * MS_PER_DAY
        return elapsed
    } /**
     * Returns the time elapsed so far this week in milliseconds.
     *
     * @return Time elapsed this week in milliseconds.
     */
    val elapsedWeek: Long
        get() {
            // Today + days passed this week
            var elapsed = elapsedToday
            val passedWeekdays = calendar[Calendar.DAY_OF_WEEK] - 1 - calendar.firstDayOfWeek
            if (passedWeekdays > 0) {
                elapsed += passedWeekdays * MS_PER_DAY
            }
            return elapsed
        } /**
     * Returns the time elapsed so far this month in milliseconds.
     *
     * @return Time elapsed this month in milliseconds.
     */
    val elapsedMonth: Long
        // Today + rest of this month
        get() = elapsedToday + (calendar[Calendar.DAY_OF_MONTH] - 1) * MS_PER_DAY

    /**
     * Returns the time elapsed so far this month and the last numMonths months in milliseconds.
     *
     * @param numMonths Additional number of months prior to the current month to calculate.
     * @return Time elapsed this month and the last numMonths months in milliseconds.
     */
    fun getElapsedMonths(numMonths: Int): Long {
        // Today + rest of this month
        var elapsed = elapsedMonth

        // Previous numMonths months
        var month = calendar[Calendar.MONTH]
        var year = calendar[Calendar.YEAR]
        for (i in 0 until numMonths) {
            month--
            if (month < Calendar.JANUARY) {
                month = Calendar.DECEMBER
                year--
            }
            elapsed += getDaysInMonth(year, month) * MS_PER_DAY
        }
        return elapsed
    }

    /**
     * Returns the time elapsed so far this year in milliseconds.
     *
     * @return Time elapsed this year in milliseconds.
     */
    val elapsedYear: Long
        get() {
            // Today + rest of this month + previous months until January
            var elapsed = elapsedMonth
            var month = calendar[Calendar.MONTH] - 1
            val year = calendar[Calendar.YEAR]
            while (month > Calendar.JANUARY) {
                elapsed += getDaysInMonth(year, month) * MS_PER_DAY
                month--
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
    private fun getDaysInMonth(@Suppress("UNUSED_PARAMETER") year: Int, month: Int): Int {
        val monthCal: Calendar = GregorianCalendar(calendar[Calendar.YEAR], month, 1)
        return monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private const val MS_PER_MINUTE = (60 * 1000).toLong()
    private const val MS_PER_DAY = 24 * 60 * MS_PER_MINUTE
}
