/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.migrate

import player.phonograph.coil.CustomArtistImageStore
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.model.pages.Pages
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.settings.dataStore
import player.phonograph.util.debug
import player.phonograph.util.file.moveFile
import player.phonograph.util.reportError
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FilenameFilter

fun migrate(context: Context, from: Int, to: Int) {

    when (from) {
        in 1 until 454   -> { // v0.5.4.1
            throw IllegalStateException("You are upgrading from a very old version (version $from)! Please Wipe app data!")
        }

        in 313 until 1000 -> { // v1.0.0
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
            migrate(LegacyClickPreferencesMigration())
            migrate(PagesMigration())
            migrate(LockScreenCoverMigration())
            migrate(AutoDownloadMetadataMigration())
            migrate(LegacyLastAddedCutoffIntervalMigration())
            migrate(CustomArtistImageStoreMigration())
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
        return from <= to && from != -1 && introduced in from + 1..to
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

private class LegacyClickPreferencesMigration : Migration(introduced = 402) {
    override fun doMigrate(context: Context) {
        removePreference(context, keyName = DeprecatedPreference.LegacyClickPreference.REMEMBER_SHUFFLE)
        removePreference(context, keyName = DeprecatedPreference.LegacyClickPreference.KEEP_PLAYING_QUEUE_INTACT)
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

private class AutoDownloadMetadataMigration : Migration(introduced = 1011) {
    override fun doMigrate(context: Context) {
        removePreference(context, keyName = DeprecatedPreference.AutoDownloadMetadata.AUTO_DOWNLOAD_IMAGES_POLICY)
    }
}

private class LegacyLastAddedCutoffIntervalMigration : Migration(introduced = 1011) {
    override fun doMigrate(context: Context) {
        removePreference(context, keyName = DeprecatedPreference.LegacyLastAddedCutoffInterval.LEGACY_LAST_ADDED_CUTOFF)
    }
}

/**
 * Custom Artist images have been moved to external storage from internal storage
 */
private class CustomArtistImageStoreMigration : Migration(introduced = 1053) {
    override fun doMigrate(context: Context) {
        val newLocation = CustomArtistImageStore.directory(context) ?: return // no external storage
        val oldLocation = CustomArtistImageStore.directoryFallback(context)
        if (!oldLocation.exists()) oldLocation.mkdirs()
        try {
            val files: Array<File> = oldLocation.listFiles(imageNameFilter) ?: return // empty
            for (file in files) {
                moveFile(file, File(newLocation, file.name))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val imageNameFilter = FilenameFilter { _, name -> name.endsWith("jpeg") }
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
    var type: Int = -1
    var exception: Exception? = null
    try {
        CoroutineScope(SupervisorJob()).launch {
            context.dataStore.edit {
                val booleanKey = booleanPreferencesKey(keyName)
                val stringKey = stringPreferencesKey(keyName)
                val intKey = intPreferencesKey(keyName)
                val longKey = intPreferencesKey(keyName)
                val keys: List<Preferences.Key<*>> = listOf(booleanKey, stringKey, intKey, longKey)
                for (key in keys) {
                    if (it.contains(key)) {
                        it.remove(key)
                        break
                    }
                }
            }
        }
    } catch (e: Exception) {
        exception = e
        type = DATASTORE
    }
    try {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        pref.edit().remove(keyName).apply()
    } catch (e: Exception) {
        exception = e
        type = PREFERENCE
    }
    if (exception != null) {
        reportError(
            exception,
            TAG,
            "Failed to remove legacy preference item `$keyName` via ${if (type == DATASTORE) "datastore" else "preference"}"
        )
    }
}

private const val PREFERENCE = 1
private const val DATASTORE = 2

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

    // "removed since version code 262"
    object MusicChooserPreference {
        const val LAST_MUSIC_CHOOSER = "last_music_chooser"
    }

    // "removed since version code 402"
    object LegacyClickPreference {
        const val REMEMBER_SHUFFLE = "remember_shuffle"
        const val KEEP_PLAYING_QUEUE_INTACT = "keep_playing_queue_intact"
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


    // "removed Auto Download Metadata from last.fm since version code 1011"
    object AutoDownloadMetadata {
        const val AUTO_DOWNLOAD_IMAGES_POLICY = "auto_download_images_policy"
        const val DOWNLOAD_IMAGES_POLICY_ALWAYS = "always"
        const val DOWNLOAD_IMAGES_POLICY_ONLY_WIFI = "only_wifi"
        const val DOWNLOAD_IMAGES_POLICY_NEVER = "never"
    }

    // "replaced with the flexible one since version code 1011"
    object LegacyLastAddedCutoffInterval {
        const val LEGACY_LAST_ADDED_CUTOFF = "last_added_interval"
        const val INTERVAL_TODAY = "today"
        const val INTERVAL_PAST_SEVEN_DAYS = "past_seven_days"
        const val INTERVAL_PAST_FOURTEEN_DAYS = "past_fourteen_days"
        const val INTERVAL_PAST_ONE_MONTH = "past_one_month"
        const val INTERVAL_PAST_THREE_MONTHS = "past_three_months"
        const val INTERVAL_THIS_WEEK = "this_week"
        const val INTERVAL_THIS_MONTH = "this_month"
        const val INTERVAL_THIS_YEAR = "this_year"
    }
}