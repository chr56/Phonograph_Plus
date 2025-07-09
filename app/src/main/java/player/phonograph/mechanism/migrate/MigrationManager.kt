/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.migrate

import player.phonograph.foundation.error.warning
import player.phonograph.model.migration.VersionMigrationRule
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.util.currentVersionCode
import player.phonograph.util.debug
import android.content.Context
import android.util.Log

object MigrationManager {

    const val CODE_SUCCESSFUL = 0
    const val CODE_NO_ACTION = 1
    const val CODE_WARNING = 100
    const val CODE_FORBIDDEN = -100
    const val CODE_UNKNOWN_ERROR = -1

    fun shouldMigration(context: Context): Boolean {
        val currentVersion = currentVersionCode(context)
        val previousVersion = PrerequisiteSetting.instance(context).previousVersion
        return currentVersion != previousVersion
    }

    fun migrate(context: Context): Int {

        val from = PrerequisiteSetting.instance(context).previousVersion
        val to = currentVersionCode(context)

        var status = CODE_SUCCESSFUL

        when (from) {
            in 1 until 1064    -> { // v1.7.0 (dev2)
                return CODE_FORBIDDEN
            }

            in 1064 until 1084 -> { // v1.8.4
                status = CODE_WARNING
            }
        }

        if (from == to) {
            debug { Log.i(TAG, "No Need to Migrate") }
            return CODE_NO_ACTION
        }

        // Actual migration

        Log.i(TAG, "Start Migration: $from -> $to")

        try {
            MigrateExecutor(context, from, to).apply {
                migrate(LegacyDetailDialogMigrationRule())
                migrate(PlaylistFilesOperationBehaviourMigrationRule())
                migrate(ColoredSystemBarsMigrationRule())
                migrate(PreloadImagesMigrationRule())
                migrate(NowPlayingScreenMigrationRule())
            }

            Log.i(TAG, "End Migration")

            PrerequisiteSetting.instance(context).previousVersion = to // todo

        } catch (e: Exception) {
            warning(context, TAG, "Failed to migrate", e)
            return CODE_UNKNOWN_ERROR
        }

        return status
    }

    private class MigrateExecutor(
        private val context: Context,
        private val from: Int,
        private val to: Int,
    ) {
        fun migrate(migration: VersionMigrationRule) {
            if (migration.check(context, from, to)) {
                Log.i(TAG, "Migrating ${migration.javaClass.simpleName} ...")
                migration.execute(context)
            }
        }
    }

    private const val TAG = "VersionMigrate"
}