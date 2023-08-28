/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import android.content.Context

interface Loader<T> {
    fun all(context: Context): List<T>
    fun id(context: Context, id: Long): T?
}