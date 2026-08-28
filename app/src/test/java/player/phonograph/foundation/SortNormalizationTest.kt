/*
 *  Copyright (c) 2026 chr_56
 */

package player.phonograph.foundation

import org.junit.Assert.assertEquals
import org.junit.Test

class SortNormalizationTest {

    @Test
    fun accentedNameSortsWithUnaccentedEquivalent() {
        val names = listOf("Mossy Ledge", "Móler")

        val sorted = names.sort(revert = false) { name -> name.normalizeForSort() }

        assertEquals(listOf("Móler", "Mossy Ledge"), sorted)
    }

    @Test
    fun commonDiacriticsNormalizeToBaseLetters() {
        assertEquals("eclair", "Éclair".normalizeForSort())
        assertEquals("uber", "Über".normalizeForSort())
        assertEquals("nandu", "Ñandú".normalizeForSort())
        assertEquals("ake", "Åke".normalizeForSort())
    }

    @Test
    fun normalizedNamesInterleaveWithAsciiNames() {
        val names = listOf("Núria", "Nadia", "Ñandú", "Åke", "Éclair", "Edda")

        val sorted = names.sort(revert = false) { name -> name.normalizeForSort() }

        assertEquals(listOf("Åke", "Éclair", "Edda", "Nadia", "Ñandú", "Núria"), sorted)
    }

    @Test
    fun normalizationIsCaseInsensitive() {
        val names = listOf("mossy ledge", "MÓLER")

        val sorted = names.sort(revert = false) { name -> name.normalizeForSort() }

        assertEquals("moler", "MÓLER".normalizeForSort())
        assertEquals(listOf("MÓLER", "mossy ledge"), sorted)
    }

    @Test
    fun emptyAndCombiningOnlyStringsHaveStableKeys() {
        assertEquals("", "".normalizeForSort())
        assertEquals("", "\u0301\u0327\u0308".normalizeForSort())
    }

    @Test
    fun nonLatinNamesPassThroughAndSortDeterministically() {
        val names = listOf("東京", "北京")

        val sorted = names.sort(revert = false) { name -> name.normalizeForSort() }

        assertEquals("東京", "東京".normalizeForSort())
        assertEquals(listOf("北京", "東京"), sorted)
    }
}
