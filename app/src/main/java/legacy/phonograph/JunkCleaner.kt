/*
 * Copyright (c) 2022 chr_56
 */

package legacy.phonograph

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import legacy.phonograph.DeprecatedPreference.LIBRARY_CATEGORIES
import legacy.phonograph.DeprecatedPreference.SortOrder.ALBUM_SONG_SORT_ORDER
import legacy.phonograph.DeprecatedPreference.SortOrder.ALBUM_SORT_ORDER
import legacy.phonograph.DeprecatedPreference.SortOrder.ARTIST_ALBUM_SORT_ORDER
import legacy.phonograph.DeprecatedPreference.SortOrder.ARTIST_SONG_SORT_ORDER
import legacy.phonograph.DeprecatedPreference.SortOrder.ARTIST_SORT_ORDER
import legacy.phonograph.DeprecatedPreference.SortOrder.GENRE_SORT_ORDER
import legacy.phonograph.DeprecatedPreference.SortOrder.SONG_SORT_ORDER
import player.phonograph.R
import player.phonograph.notification.BackgroundNotification
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.service.util.QueuePreferenceManager.Companion.NAME as QUEUE_PREF
import player.phonograph.util.CoroutineUtil
import android.content.Context.MODE_PRIVATE

class JunkCleaner(context: Context) : FileCleaner(context) {
    companion object {
        private const val pref_ath = "[[kabouzeid_app-theme-helper]]"

        private const val NOTIFICATION_ID = 8598691
    }

    override fun clear(versionCode: Int, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.IO + exceptionHandler) {
            BackgroundNotification.post(context.getString(R.string.deleting_old_files), "", NOTIFICATION_ID)
            removePreferenceFile(versionCode)
            removeDeprecatedPreference(versionCode)
            migrateQueuePreferences(versionCode)
            BackgroundNotification.remove(NOTIFICATION_ID)
        }
    }

    fun removePreferenceFile(versionCode: Int) {
        if (versionCode >= 100) deletePreferenceFile(context, name = pref_ath)
    }

    fun removeDeprecatedPreference(versionCode: Int) {
        if (versionCode >= 101) removePreference(context, keyName = LIBRARY_CATEGORIES)
        if (versionCode >= 210) {
            removePreference(context, keyName = ARTIST_SORT_ORDER)
            removePreference(context, keyName = ARTIST_SONG_SORT_ORDER)
            removePreference(context, keyName = ARTIST_ALBUM_SORT_ORDER)
            removePreference(context, keyName = ALBUM_SORT_ORDER)
            removePreference(context, keyName = ALBUM_SONG_SORT_ORDER)
            removePreference(context, keyName = SONG_SORT_ORDER)
            removePreference(context, keyName = GENRE_SORT_ORDER)
        }
    }

    fun migrateQueuePreferences(versionCode: Int) {
        Log.i(TAG, "Start Migrate Queue Preferences")

        fun moveIntPreference(
            oldKeyName: String,
            newKeyName: String
        ) {
            try {
                val oldPref = PreferenceManager.getDefaultSharedPreferences(context)
                val value = oldPref.getInt(oldKeyName, 0)
                val newPref = context.getSharedPreferences(QUEUE_PREF, MODE_PRIVATE)
                newPref.edit().putInt(newKeyName, value).apply()
                oldPref.edit().remove(oldKeyName).apply()

                Log.i(TAG, "Success: $oldKeyName -> $newKeyName")
            } catch (e: Exception) {
                Log.i(TAG, "Fail: $oldKeyName -> $newKeyName")
            }
        }
        if (versionCode > 454) {
            moveIntPreference(
                DeprecatedPreference.QueueCfg.PREF_REPEAT_MODE,
                QueuePreferenceManager.KEY_REPEAT_MODE
            )
            moveIntPreference(
                DeprecatedPreference.QueueCfg.PREF_SHUFFLE_MODE,
                QueuePreferenceManager.KEY_SHUFFLE_MODE
            )
            moveIntPreference(
                DeprecatedPreference.QueueCfg.PREF_POSITION,
                QueuePreferenceManager.KEY_CURRENT_POSITION
            )
            moveIntPreference(
                DeprecatedPreference.QueueCfg.PREF_POSITION_IN_TRACK,
                QueuePreferenceManager.KEY_CURRENT_MILLISECOND
            )
        }
        Log.i(TAG, "Migrate Queue Preferences Completed")
    }

}

object DeprecatedPreference {
    // "removed since version code 101"
    const val LIBRARY_CATEGORIES = "library_categories"

    // "removed since version code 210"
    object SortOrder {
        const val ARTIST_SORT_ORDER = "artist_sort_order"
        const val ARTIST_SONG_SORT_ORDER = "artist_song_sort_order"
        const val ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order"
        const val ALBUM_SORT_ORDER = "album_sort_order"
        const val ALBUM_SONG_SORT_ORDER = "album_song_sort_order"
        const val SONG_SORT_ORDER = "song_sort_order"
        const val GENRE_SORT_ORDER = "genre_sort_order"
    }

    // "move to a separate preference after 454"
    object QueueCfg {
        const val PREF_POSITION = "POSITION"
        const val PREF_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val PREF_REPEAT_MODE = "REPEAT_MODE"
        const val PREF_POSITION_IN_TRACK = "POSITION_IN_TRACK"
    }
}

@Suppress("SameParameterValue")
abstract class FileCleaner(val context: Context) {
    abstract fun clear(versionCode: Int, coroutineScope: CoroutineScope)

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
        CoroutineScope(SupervisorJob()).launch {
            CoroutineUtil.coroutineToast(context, R.string.failed_to_delete)
        }
    }

    protected fun logSuccess(str: String) {
        Log.i(TAG, "$str was deleted!")
    }

    protected val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, e ->
            Log.e(TAG, e.stackTraceToString())
            ErrorNotification.postErrorNotification(e, "failed to delete old files!\n")
        }
    }

    companion object {
        const val TAG = "FileCleaner"
    }
}
