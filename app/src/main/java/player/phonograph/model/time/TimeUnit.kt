/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.time


private const val SYMBOL_YEAR = 'Y'
private const val SYMBOL_MONTH = 'M'
private const val SYMBOL_WEEK = 'W'
private const val SYMBOL_DAY = 'D'
private const val SYMBOL_HOUR = 'h'
private const val SYMBOL_MINUTE = 'm'
private const val SYMBOL_SECOND = 's'


enum class TimeUnit(val symbol: Char) {
    Year(SYMBOL_YEAR),
    Month(SYMBOL_MONTH),
    Week(SYMBOL_WEEK),
    Day(SYMBOL_DAY),
    Hour(SYMBOL_HOUR),
    Minute(SYMBOL_MINUTE),
    Second(SYMBOL_SECOND),
    ;


    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        //region Const for static convert
        const val DAYS_PER_WEEK = 7

        const val HOURS_PER_DAY = 24
        const val HOUR_PER_WEEK = DAYS_PER_WEEK * HOURS_PER_DAY

        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY
        const val MINUTES_PER_WEEK = MINUTES_PER_DAY * DAYS_PER_WEEK

        const val SECONDS_PER_MINUTE = 60
        const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
        const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY
        const val SECONDS_PER_WEEK = SECONDS_PER_DAY * DAYS_PER_WEEK
        //endregion

        const val DAYS_PER_AVERAGE_MONTH = 30
        const val DAYS_PER_AVERAGE_YEAR = 365


        private const val BASE_THOUSAND = 1000
        const val MILLI_PER_SECOND = BASE_THOUSAND
        const val NANOS_PER_MILLI = BASE_THOUSAND * BASE_THOUSAND
        const val NANOS_PER_SECOND = BASE_THOUSAND * BASE_THOUSAND * BASE_THOUSAND

        const val MILLI_PER_MINUTE = MILLI_PER_SECOND * SECONDS_PER_MINUTE
        const val MILLI_PER_HOUR = MILLI_PER_SECOND * SECONDS_PER_HOUR
        const val MILLI_PER_DAY = MILLI_PER_SECOND * SECONDS_PER_DAY
        const val MILLI_PER_WEEK = MILLI_PER_SECOND * SECONDS_PER_WEEK


        fun from(symbol: Char): TimeUnit = when (symbol) {
            SYMBOL_YEAR   -> Year
            SYMBOL_MONTH  -> Month
            SYMBOL_WEEK   -> Week
            SYMBOL_DAY    -> Day
            SYMBOL_HOUR   -> Hour
            SYMBOL_MINUTE -> Minute
            SYMBOL_SECOND -> Second
            else          -> throw IllegalArgumentException("Unknown time unit $symbol!")
        }

    }
}