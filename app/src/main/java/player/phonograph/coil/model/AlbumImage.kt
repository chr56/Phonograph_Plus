/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.model

data class AlbumImage(
    override val id: Long,
    val name: String,
    val files: List<SongImage>,
) : CompositeLoaderTarget<SongImage> {

    override fun disassemble(): Iterable<SongImage> = files

    override fun toString(): String =
        "AlbumImage(name=$name, id=$id, files=${files.joinToString(prefix = "[", postfix = "]") { it.path }})"
}