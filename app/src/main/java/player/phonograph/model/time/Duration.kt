/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.time

sealed class Duration(val value: Long, val unit: TimeUnit) {
    init {
        require(value >= 0)
    }

    class Second(value: Long) : Duration(value, TimeUnit.Second)
    class Minute(value: Long) : Duration(value, TimeUnit.Minute)
    class Hour(value: Long) : Duration(value, TimeUnit.Hour)
    class Day(value: Long) : Duration(value, TimeUnit.Day)
    class Week(value: Long) : Duration(value, TimeUnit.Week)
    class Month(value: Long) : Duration(value, TimeUnit.Month)
    class Year(value: Long) : Duration(value, TimeUnit.Year)

    fun toSeconds(): Long {
        return when (this) {
            is Second -> value
            is Minute -> value * TimeUnit.SECONDS_PER_MINUTE
            is Hour   -> value * TimeUnit.SECONDS_PER_HOUR
            is Day    -> value * TimeUnit.SECONDS_PER_DAY
            is Week   -> value * TimeUnit.SECONDS_PER_WEEK
            is Month  -> value * TimeUnit.SECONDS_PER_DAY * TimeUnit.DAYS_PER_AVERAGE_MONTH
            is Year   -> value * TimeUnit.SECONDS_PER_DAY * TimeUnit.DAYS_PER_AVERAGE_YEAR
        }
    }

    override fun toString(): String = "Duration{ $value ${unit.name}}"

    override fun hashCode(): Int =
        100003 * unit.hashCode() + value.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Duration) return false

        if (value != other.value) return false
        if (unit != other.unit) return false

        return true
    }

    fun serialise() =
        "$value${unit.symbol}"

    companion object {
        fun of(value: Long, unit: TimeUnit): Duration = when (unit) {
            TimeUnit.Year   -> Year(value)
            TimeUnit.Month  -> Month(value)
            TimeUnit.Week   -> Week(value)
            TimeUnit.Day    -> Day(value)
            TimeUnit.Hour   -> Hour(value)
            TimeUnit.Minute -> Minute(value)
            TimeUnit.Second -> Second(value)
        }

        fun from(str: String): Duration? {
            if (str.length <= 1) return null
            val symbol = str.last()
            val timeUnit = TimeUnit.from(symbol)
            val value = str.dropLast(1).toLongOrNull() ?: return null
            return of(value, timeUnit)
        }
    }

}