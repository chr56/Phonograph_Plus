/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util

private const val SID_SHIFT = Long.SIZE_BITS - Byte.SIZE_BITS // 56
private const val SID_MASK_LOWER = (1L shl SID_SHIFT) - 1 // 0x00ff_ffff_ffff_ffff
/**
 * Generate a sectioned 64-bits ID: the lowers 56 bits is cut from [id], the higher 8 bits is shifted from [section]
 */
fun produceSectionedId(id: Long, section: Int): Long {
    return (id and SID_MASK_LOWER) + (section shl SID_SHIFT)
}

private const val ID_SHIFT: Int = 36 // 4 * 9
private const val ID_EMBED_SIZE: Int = 20 // 4 * 5
private const val ID_MASK_CUT: Long = (1L shl (ID_EMBED_SIZE)) - 1 // 0x000_0000_000f_ffff
private const val ID_MASK_MID: Long = (ID_MASK_CUT shl ID_SHIFT) // 0x00ff_fff0_0000_0000

/**
 * Generate a new ID associated with a position, making it safe to used in some lists allowing duplicated item
 * @param id original id
 * @param position related position
 * @return new id which is safe to used in a list allowing duplicated item
 */
fun produceSafeId(id: Long, position: Int): Long {
    val cleared: Long = id and ID_MASK_MID.inv()
    val shifted: Long = (position.toLong() and ID_MASK_CUT) shl ID_SHIFT
    return cleared or shifted
}

fun isEmbeddingOverflow(id: Long): Boolean {
    val eased: Long = id and ID_MASK_MID
    return eased != 0L
}