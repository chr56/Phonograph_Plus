/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.migration

import android.content.Context


abstract class VersionMigrationRule(
    val introduced: Int,
    val deprecated: Int = Int.MAX_VALUE,
) : MigrationRule {

    /**
     * check condition of migration rule
     * @param from previous version code
     * @param to current version code
     */
    open fun check(context: Context, from: Int, to: Int): Boolean {
        return from <= to && from != -1 && introduced in from + 1..to
    }

}