/*
 * Copyright (c) 2022 chr_56
 */

package legacy.phonograph

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.util.Util

object JunkCleaner {

    // todo use when bumping version
    fun removePreferenceFile(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.deleteSharedPreferences(ath)
            else
                context.getSharedPreferences(ath, Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Fail to clean old Preference")
            GlobalScope.launch {
                Util.coroutineToast(context, "Failed to delete old Preference files!")
            }
        }
    }

    private const val ath = "[[kabouzeid_app-theme-helper]]"

    private const val TAG = "JunkCleaner"
}
