/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.migrate

import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.model.pages.Pages
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.util.debug
import player.phonograph.util.reportError
import androidx.preference.PreferenceManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log

fun migrate(context: Context, from: Int, to: Int) {

    when (from) {
        in 1 until 225   -> {
            throw IllegalStateException("You are upgrading from a very old version (version $from)! Please Wipe app data!")
        }
        in 210 until 500 -> {
            reportError(
                IllegalStateException(), TAG,
                "You are upgrading from a very old version (version $from)! Try to wipe app data!"
            )
        }
    }

    if (from != to) {
        Log.i(TAG, "Start Migration: $from -> $to")

        MigrateOperator(context, from, to).apply {
            migrate(QueuePreferenceMigration())
            migrate(PagesMigration())
            migrate(LockScreenCoverMigration())
        }

        Log.i(TAG, "End Migration")

        PrerequisiteSetting.instance(context).previousVersion = to
    } else {
        debug {
            Log.i(TAG, "No Need to Migrate")
        }
    }
}

/**
 * Migration Rule
 */
private abstract class Migration(
    val introduced: Int,
    val deprecated: Int = Int.MAX_VALUE,
) {

    /**
     * actual codes that operate migrations
     */
    abstract fun doMigrate(context: Context)

    /**
     * check condition of migrate
     */
    fun check(from: Int, to: Int): Boolean {
        return from <= to && introduced in from + 1..to
    }

    fun tryMigrate(context: Context, from: Int, to: Int) {
        if (check(from, to)) {
            doMigrate(context)
            Log.i(TAG, "Migrating: ${javaClass.simpleName}")
        }
    }
}


private class MigrateOperator(
    private val context: Context,
    private val from: Int,
    private val to: Int,
) {
    fun migrate(migration: Migration) =
        migration.tryMigrate(context, from, to)
}

private class QueuePreferenceMigration : Migration(introduced = 460, deprecated = 532) {
    override fun doMigrate(context: Context) {
        fun moveTargetPreference(oldKeyName: String, newKeyName: String) =
            moveIntPreference(
                PreferenceManager.getDefaultSharedPreferences(context),
                oldKeyName,
                context.getSharedPreferences(QueuePreferenceManager.NAME, MODE_PRIVATE),
                newKeyName
            )
        moveTargetPreference(
            DeprecatedPreference.QueueCfg.PREF_REPEAT_MODE,
            QueuePreferenceManager.KEY_REPEAT_MODE
        )
        moveTargetPreference(
            DeprecatedPreference.QueueCfg.PREF_SHUFFLE_MODE,
            QueuePreferenceManager.KEY_SHUFFLE_MODE
        )
        moveTargetPreference(
            DeprecatedPreference.QueueCfg.PREF_POSITION,
            QueuePreferenceManager.KEY_CURRENT_POSITION
        )
        moveTargetPreference(
            DeprecatedPreference.QueueCfg.PREF_POSITION_IN_TRACK,
            QueuePreferenceManager.KEY_CURRENT_MILLISECOND
        )
    }
}

private class PagesMigration : Migration(introduced = 460) {
    override fun doMigrate(context: Context) {
        HomeTabConfig.append(Pages.FOLDER)
    }
}

private class LockScreenCoverMigration : Migration(introduced = 522, deprecated = 531) {
    override fun doMigrate(context: Context) {
        removePreference(context, keyName = DeprecatedPreference.LockScreenCover.ALBUM_ART_ON_LOCKSCREEN)
        removePreference(context, keyName = DeprecatedPreference.LockScreenCover.BLURRED_ALBUM_ART)
    }
}

private fun moveIntPreference(
    oldPreference: SharedPreferences,
    oldKeyName: String,
    newPreference: SharedPreferences,
    newKeyName: String,
) {
    try {
        val value = oldPreference.getInt(oldKeyName, 0)
        newPreference.edit().putInt(newKeyName, value).apply()
        oldPreference.edit().remove(oldKeyName).apply()
        Log.i(TAG, "Success: $oldKeyName -> $newKeyName")
    } catch (e: Exception) {
        Log.i(TAG, "Fail: $oldKeyName -> $newKeyName")
    }
}


private fun removePreference(context: Context, keyName: String) {
    try {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        pref.edit().remove(keyName).apply()
    } catch (e: Exception) {
        reportError(e, TAG, " failed: removing old Preference item `$keyName`")
    }
}

private const val TAG = "VersionMigrate"

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

    // "move to a separate preference since 460"
    object QueueCfg {
        const val PREF_POSITION = "POSITION"
        const val PREF_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val PREF_REPEAT_MODE = "REPEAT_MODE"
        const val PREF_POSITION_IN_TRACK = "POSITION_IN_TRACK"
    }

    // "remove lockscreen cover since 522"
    object LockScreenCover {
        const val ALBUM_ART_ON_LOCKSCREEN = "album_art_on_lockscreen"
        const val BLURRED_ALBUM_ART = "blurred_album_art"
    }
}