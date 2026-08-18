/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation

import kotlin.random.Random
import java.util.concurrent.atomic.AtomicLong

class SafeIdentifierGenerator(seed: Long) {

    private val random by lazy(LazyThreadSafetyMode.NONE) { Random(seed) }
    private val accumulator by lazy { AtomicLong(0) }

    /**
     * Check and acquire a safe id.
     * @param id the original id, may be invalid
     * @return a safe id that is not likely to conflict
     */
    fun acquire(id: Long = -1): Long =
        if (id > 0) id else random.nextLong(1, Long.MAX_VALUE)

    /**
     * generate a sequential id
     */
    fun generate(): Long = accumulator.incrementAndGet()

    companion object {
        private const val ID_SHIFT: Int = 36 // 4 * 9
        private const val ID_EMBED_SIZE: Int = 20 // 4 * 5
        private const val ID_MASK_CUT: Long = (1L shl (ID_EMBED_SIZE)) - 1 // 0x000_0000_000f_ffff
        private const val ID_MASK_MID: Long = (ID_MASK_CUT shl ID_SHIFT) // 0x00ff_fff0_0000_0000

        /**
         * Generate a new id associated with a position, making it safe to used in some lists allowing duplicated item
         * @param id the original id, MUST BE UNIQUE AND VALID
         * @param position related position
         * @return new id which is safe to used in a list allowing duplicated item
         */
        fun make(id: Long, position: Int): Long {
            val cleared: Long = id and ID_MASK_MID.inv()
            val shifted: Long = (position.toLong() and ID_MASK_CUT) shl ID_SHIFT
            return cleared or shifted
        }

        fun checkEmbeddingOverflowed(id: Long): Boolean {
            val eased: Long = id and ID_MASK_MID
            return eased != 0L
        }

        fun checkInvalidation(id: Long): Boolean = id <= 0
    }
}