/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata

import org.jaudiotagger.tag.FieldKey
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import kotlin.enums.EnumEntries

object JAudioTaggerMetadataKeyTranslator {
    fun ConventionalMusicMetadataKey.toFieldKey(): FieldKey = FieldKey.entries[ordinal]
    fun FieldKey.toMusicMetadataKey(): ConventionalMusicMetadataKey = ConventionalMusicMetadataKey.entries[ordinal]

    val AllFieldKey: EnumEntries<FieldKey> get() = FieldKey.entries
}