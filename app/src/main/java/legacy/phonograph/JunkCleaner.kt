/*
 * Copyright (c) 2022 chr_56
 */

package legacy.phonograph

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.R
import player.phonograph.settings.Setting
import player.phonograph.util.Util
import kotlin.math.log

class JunkCleaner(context: Context) : FileCleaner(context) {
    companion object {
        private const val pref_ath = "[[kabouzeid_app-theme-helper]]"
    }

    override fun clear(versionCode: Int) {
        GlobalScope.launch {
            Util.coroutineToast(context, R.string.deleting_old_files)
        }
        removePreferenceFile(versionCode)
        removeDeprecatedPreference(versionCode)
    }

    // todo use when bumping version
    fun removePreferenceFile(versionCode: Int) {
        if (versionCode >= 100) deletePreferenceFile(context, name = pref_ath)
    }

    fun removeDeprecatedPreference(versionCode: Int) {
        if (versionCode >= 101) removePreference(context, keyName = Setting.LIBRARY_CATEGORIES)
    }
}

@Suppress("SameParameterValue")
abstract class FileCleaner(val context: Context) {
    abstract fun clear(versionCode: Int)

    protected fun removePreference(context: Context, keyName: String) {
        try {
            logStart(keyName)
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            pref.edit().remove(keyName).apply()
            logSuccess("Preference Key $keyName")
        } catch (e: Exception) {
            logFail("old Preference item \"$keyName\"")
        }
    }

    protected fun deletePreferenceFile(context: Context, name: String) {
        try {
            logStart(name)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(name)
            } else {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
                true
            }
                .let { result -> if (result) logSuccess(name) }
        } catch (e: Exception) {
            logFail("old Preference file \"$name\"")
        }
    }

    protected fun logStart(str: String) {
        Log.i(TAG, "Start cleaning $str")
    }

    protected fun logFail(str: String) {
        Log.e(TAG, "Failed to clean $str ...")
        GlobalScope.launch {
            Util.coroutineToast(context, R.string.failed_to_delete)
        }
    }

    protected fun logSuccess(str: String) {
        Log.i(TAG, "$str was deleted!")
    }

    companion object {
        const val TAG = "FileCleaner"
    }
}
