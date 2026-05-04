/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.model

import android.content.Context

interface CompositeLoaderTarget<I> : LoaderTarget {
    suspend fun items(context: Context): Iterable<I>
}