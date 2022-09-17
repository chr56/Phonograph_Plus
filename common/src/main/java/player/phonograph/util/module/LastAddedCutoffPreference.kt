/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.module

import player.phonograph.BaseApp
import player.phonograph.util.CalendarUtil

object LastAddedCutoffPreference {
    private const val IGNORE_MEDIA_STORE_ARTWORK = "ignore_media_store_artwork"

    private val impl: StringIsolatePreference =
        StringIsolatePreference(IGNORE_MEDIA_STORE_ARTWORK, "past_one_month", BaseApp.instance)

    var lastAddedCutoffPref: String?
        get() = impl.read()
        set(newValue) {
            impl.write(newValue)
        }
    val lastAddedCutoff: Long
        get() {
            val interval: Long = when (lastAddedCutoffPref) {
                "today" -> CalendarUtil.elapsedToday
                "past_seven_days" -> CalendarUtil.getElapsedDays(7)
                "past_fourteen_days" -> CalendarUtil.getElapsedDays(14)
                "past_one_month" -> CalendarUtil.getElapsedMonths(1)
                "past_three_months" -> CalendarUtil.getElapsedMonths(3)
                "this_week" -> CalendarUtil.elapsedWeek
                "this_month" -> CalendarUtil.elapsedMonth
                "this_year" -> CalendarUtil.elapsedYear
                else -> CalendarUtil.getElapsedMonths(1)
            }
            return (System.currentTimeMillis() - interval) / 1000
        }
}