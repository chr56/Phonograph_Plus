/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.migration

import android.content.Context

/**
 * Define a migration rule
 */
sealed interface MigrationRule {

    /**
     * execute this migration rule
     */
    fun execute(context: Context)

}