/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.model

import android.content.Context

interface CompositeLoaderTarget<I> : LoaderTarget {
    suspend fun items(context: Context): Iterable<I>
}