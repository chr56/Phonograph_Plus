/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.model

interface CompositeLoaderTarget<I> : LoaderTarget {
    fun disassemble(): Iterable<I>
}