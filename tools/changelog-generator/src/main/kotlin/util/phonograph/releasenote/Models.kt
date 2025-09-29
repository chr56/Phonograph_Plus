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

    val tag: String get() = tagOf(version, channel)
    val previousTag: String get() = tagOf(previousVersion, previousChannel)

    fun variant(variant: TargetVariant): String = variantOf(variant, channel)

    fun language(lang: Language): Notes.Note = when (lang) {
        Language.EN -> notes.en
        Language.ZH -> notes.zh
    }

    companion object {
        private fun tagOf(version: String, channel: ReleaseChannel): String =
            when (channel) {
                ReleaseChannel.PREVIEW -> "preview_$version"
                ReleaseChannel.STABLE  -> "v$version"
                ReleaseChannel.LTS     -> "v$version"
            }

        private fun variantOf(
            variant: TargetVariant,
            channel: ReleaseChannel,
            buildType: BuildType = BuildType.RELEASE,
        ): String =
            buildString {
                append(
                    when (variant) {
                        TargetVariant.MODERN -> "Modern"
                        TargetVariant.LEGACY -> "Legacy"
                    }
                )
                append(
                    when (channel) {
                        ReleaseChannel.PREVIEW -> "Preview"
                        else                   -> "Stable"
                    }
                )
                append(
                    when (buildType) {
                        BuildType.RELEASE -> "Release"
                        BuildType.DEBUG   -> "Debug"
                    }
                )
            }
    }
}


@Serializable
data class Notes(
    val en: Note,
    val zh: Note,
) {
    @Serializable
    data class Note(
        val notice: String? = null,
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
enum class TargetVariant {
    MODERN,
    LEGACY,
    ;
}

@Serializable
enum class BuildType {
    RELEASE,
    DEBUG,
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
