/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseMetadata(
    val version: String,
    val versionCode: Int,
    val timestamp: Long,
    val channel: ReleaseChannel,
    val notes: Notes,
    val previousVersion: String,
    val previousChannel: ReleaseChannel,
) {

    val tag: String get() = channel.gitTagNameOf(version)
    val previousTag: String get() = previousChannel.gitTagNameOf(previousVersion)

    fun variant(variant: TargetVariant): String = variantQualifierOf(variant, channel, BuildType.RELEASE)

    fun language(lang: Language): Notes.Note = notes.language(lang)


    @Serializable
    data class Notes(
        val en: Note,
        val zh: Note,
    ) {

        fun language(lang: Language): Note =
            when (lang) {
                Language.EN -> en
                Language.ZH -> zh
            }

        @Serializable
        data class Note(
            val notice: String? = null,
            val highlights: List<String> = emptyList(),
            val items: List<String> = emptyList(),
        )
    }
}