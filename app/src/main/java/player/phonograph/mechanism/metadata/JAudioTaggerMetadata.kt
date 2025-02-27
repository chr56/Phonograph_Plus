/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata

import org.jaudiotagger.tag.FieldKey
import player.phonograph.mechanism.metadata.JAudioTaggerMetadataKeyTranslator.toFieldKey
import player.phonograph.mechanism.metadata.JAudioTaggerMetadataKeyTranslator.toMusicMetadataKey
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.MusicMetadata
import player.phonograph.model.metadata.TagMetadataKey

data class JAudioTaggerMetadata(
    private val _genericTagFields: Map<FieldKey, Metadata.Field>,
    override val allTagFields: Map<String, Field>,
) : MusicMetadata {

    override val genericTagFields: Map<ConventionalMusicMetadataKey, Metadata.Field>
        get() = _genericTagFields.mapKeys { it.key.toMusicMetadataKey() }

    override val textTagFields: Map<ConventionalMusicMetadataKey, Metadata.Field>
        get() = genericTagFields.filter { it.value is Metadata.PlainStringField }

    data class Key(val id: String, override val res: Int = 0) : TagMetadataKey

    data class Field(
        val id: String,
        val name: String,
        val value: Metadata.Field,
        val description: String?,
    ) : Metadata.Field by value

    override fun get(key: Metadata.Key): Metadata.Field? = when (key) {
        is ConventionalMusicMetadataKey -> _genericTagFields[key.toFieldKey()]
        is Key                          -> allTagFields[key.id]
        else                            -> null
    }

    override fun contains(key: Metadata.Key): Boolean = when (key) {
        is ConventionalMusicMetadataKey -> _genericTagFields.containsKey(key.toFieldKey())
        is Key                          -> allTagFields.containsKey(key.id)
        else                            -> false
    }

    override val fields: List<Metadata.Entry>
        get() = allTagFields.entries.map { (key, field) -> Metadata.PlainEntry(Key(key), field) }

}