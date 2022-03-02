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

class JunkCleaner(context: Context) : FileCleaner(context) {
    companion object {
        private const val pref_ath = "[[kabouzeid_app-theme-helper]]"
    }

    override fun clear(versionCode: Int) {
        GlobalScope.launch {
            Util.coroutineToast(context, "Deleting Old Files")
        }
        removePreferenceFile(versionCode)
        // more
    }

    // todo use when bumping version
    fun removePreferenceFile(versionCode: Int) {
        try {
            if (versionCode > 8) deletePreference(context, name = pref_ath)
        } catch (e: Exception) {
            reportFail("old Preference \"app-theme-helper\"")
        }
    }
}

@Suppress("SameParameterValue")
abstract class FileCleaner(val context: Context) {
    abstract fun clear(versionCode: Int)

    protected fun deletePreference(context: Context, name: String) {
        logStage(name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.deleteSharedPreferences(name)
        else
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
    }

    protected fun reportFail(str: String) {
        Log.e(TAG, "Fail to clean $str")
        GlobalScope.launch {
            Util.coroutineToast(context, "Failed to delete $str")
        }
    }
    protected fun logStage(str: String) {
        Log.i(TAG, "Start clean $str")
    }


    companion object {
        const val TAG = "FileCleaner"
    }
}
