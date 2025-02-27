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
        fun text(): CharSequence?
    }

    data object EmptyField : Field {
        override fun text() = "<Empty>"
    }

    data class PlainStringField(val data: String) : Field {
        override fun text() = data
    }

    data class PlainNumberField(val data: Long) : Field {
        override fun text() = data.toString()
    }

    data class MultipleField(val data: Collection<Field>) : Field {
        override fun text(): CharSequence? = data.map(Field::text).joinToString(separator = "\n")
    }

    interface BinaryField : Field {
        override fun text(): CharSequence = "<Binary>"
        fun binary(): ByteArray
    }

    interface Entry {
        val key: Key
        val field: Field
    }

    data class PlainEntry<K : Key, F : Field>(override val key: K, override val field: F) : Entry
}