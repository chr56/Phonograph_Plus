/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class CrashReport(
    val type: Int,
    val note: String,
    val stackTrace: String,
) : Parcelable {
    companion object Constant {
        const val KEY = "CRASH_REPORT"

        const val CRASH_TYPE_CRASH = 128
        const val CRASH_TYPE_INTERNAL_ERROR = 2
        const val CRASH_TYPE_CORRUPTED_DATA = 8
    }
}