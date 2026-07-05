/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.migration

import player.phonograph.model.repo.sync.ProgressConnection
import android.content.Context

/**
 * Define a migration rule
 */
sealed interface MigrationRule {

    /**
     * execute this migration rule
     * @param connection Callback connect to report current status
     */
    fun execute(context: Context, connection: ProgressConnection?)

}