/*
 * Copyright (c) 2022 chr_56
 */

package legacy.phonograph

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.R
import player.phonograph.util.Util

class JunkCleaner(context: Context) : FileCleaner(context) {
    companion object {
        private const val pref_ath = "[[kabouzeid_app-theme-helper]]"
    }

    override fun clear(versionCode: Int) {
        GlobalScope.launch {
            Util.coroutineToast(context, R.string.deleting_old_files)
        }
        removePreferenceFile(versionCode)
        // more
    }

    // todo use when bumping version
    fun removePreferenceFile(versionCode: Int) {
        if (versionCode > 8) deletePreference(context, name = pref_ath)
    }
}

@Suppress("SameParameterValue")
abstract class FileCleaner(val context: Context) {
    abstract fun clear(versionCode: Int)

    protected fun deletePreference(context: Context, name: String) {
        try {
            logStage(name)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(name)
            } else {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
                true
            }
                .let { result -> if (result) reportSuccess(name) }
        } catch (e: Exception) {
            reportFail("old Preference \"app-theme-helper\"")
        }
    }

    protected fun logStage(str: String) {
        Log.i(TAG, "Start clean $str")
    }

    protected fun reportFail(str: String) {
        Log.e(TAG, "Failed to clean $str ...")
        GlobalScope.launch {
            Util.coroutineToast(context, R.string.failed_to_delete)
        }
    }

    protected fun reportSuccess(str: String) {
        Log.i(TAG, "$str was deleted!")
    }

    companion object {
        const val TAG = "FileCleaner"
    }
}
