/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.time

import player.phonograph.R
import android.content.res.Resources


fun TimeIntervalCalculationMode.displayText(resources: Resources) = when (this) {
    TimeIntervalCalculationMode.PAST   -> resources.getString(R.string.interval_past)
    TimeIntervalCalculationMode.RECENT -> resources.getString(R.string.interval_recent)
}

fun TimeUnit.displayText(resources: Resources)=when(this){
    TimeUnit.Year   -> resources.getString(R.string.timeunit_year)
    TimeUnit.Month  -> resources.getString(R.string.timeunit_month)
    TimeUnit.Week   -> resources.getString(R.string.timeunit_week)
    TimeUnit.Day    -> resources.getString(R.string.timeunit_day)
    TimeUnit.Hour   -> resources.getString(R.string.timeunit_hour)
    TimeUnit.Minute -> resources.getString(R.string.timeunit_minute)
    TimeUnit.Second -> resources.getString(R.string.timeunit_second)
}