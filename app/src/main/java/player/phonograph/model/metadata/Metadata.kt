/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.metadata

import androidx.annotation.StringRes

interface Metadata {

    operator fun get(key: Key): Field?

    fun contains(key: Key): Boolean

    val fields: List<Entry>

    interface Key {
        @get:StringRes
        val res: Int
    }

    interface Field {
        val data: Any

        @MetadataNotation
        val notation: Int
    }

    data object EmptyField : Field {
        override val data: Any = Any()
        override val notation: Int = NOTATION_EMPTY
    }

    open class BinaryField(override val data: ByteArray) : Field {
        override val notation: Int = NOTATION_BINARY
    }

    open class RawTextualField(override val data: ByteArray) : Field {
        override val notation: Int = NOTATION_RAW_TEXT
    }

    data class TextualField(override val data: String) : Field {
        override val notation: Int = NOTATION_TEXT
    }

    data class NumericField(override val data: Long, @field:MetadataNotation override val notation: Int) : Field

    data class MultipleField(override val data: Collection<Field>) : Field {
        override val notation: Int = NOTATION_COMPOSITE
    }

    interface Entry {
        val key: Key
        val field: Field
    }

    data class PlainEntry<K : Key, F : Field>(override val key: K, override val field: F) : Entry
}