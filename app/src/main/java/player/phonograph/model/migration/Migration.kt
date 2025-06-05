/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.migration

import android.content.Context

/**
 * Define a migration rule
 */
abstract class Migration(
    val introduced: Int,
    val deprecated: Int = Int.MAX_VALUE,
) {

    /**
     * execute this migration rule
     */
    abstract fun execute(context: Context)

    /**
     * check condition of migration rule
     * @param from previous version code
     * @param to current version code
     */
    open fun check(from: Int, to: Int): Boolean {
        return from <= to && from != -1 && introduced in from + 1..to
    }

}