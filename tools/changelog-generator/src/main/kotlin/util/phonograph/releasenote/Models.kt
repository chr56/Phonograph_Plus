/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.releasenote

import kotlinx.serialization.Serializable


@Serializable
data class ReleaseNote(
    val version: String,
    val versionCode: Int,
    val timestamp: Long,
    val channel: ReleaseChannel,
    val notes: Notes,
    val previousVersion: String,
    val previousChannel: ReleaseChannel,
) {

    val tag: String
        get() = when (channel) {
            ReleaseChannel.PREVIEW -> "preview_$version"
            ReleaseChannel.STABLE  -> "v$version"
            ReleaseChannel.LTS     -> "v$version"
        }

    fun language(lang: Language): Notes.Note = when (lang) {
        Language.EN -> notes.en
        Language.ZH -> notes.zh
    }
}


@Serializable
data class Notes(
    val en: Note,
    val zh: Note,
) {
    @Serializable
    data class Note(
        val highlights: List<String> = emptyList(),
        val items: List<String> = emptyList(),
    )
}


@Serializable
enum class ReleaseChannel {
    STABLE,
    PREVIEW,
    LTS,
    ;
}

@Serializable
enum class Language(val displayName: String, val fullCode: String) {
    EN("English", "en-US"),
    ZH("Chinese", "zh-CN"),
    ;

    companion object {
        val ALL = listOf(EN, ZH)
    }
}
