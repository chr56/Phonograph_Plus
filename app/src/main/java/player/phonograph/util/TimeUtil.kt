/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import android.text.format.DateFormat
import java.util.*

object TimeUtil {

    fun currentDate(): Date = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).time
    fun currentTimestamp(): Long = currentDate().time

    fun currentDateTime(): CharSequence = DateFormat.format("yyMMdd_HHmmss", currentDate())

}