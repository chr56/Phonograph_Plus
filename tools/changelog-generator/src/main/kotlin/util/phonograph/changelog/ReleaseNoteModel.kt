/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

data class ReleaseNoteModel(
    val version: String,
    val versionCode: Int,
    val time: Long,
    val channel: ReleaseChannel?,
    val note: Note,
) {
    data class Note(private val map: Map<Language, List<String>>) {
        fun language(lang: Language): List<String> =
            map[lang] ?: run { println("no note for ${lang.code}"); emptyList() }
    }
}

sealed class ReleaseChannel(val name: String) {
    object STABLE : ReleaseChannel("stable")
    object PREVIEW : ReleaseChannel("preview")
    object LTS : ReleaseChannel("lts")
    class UNKNOWN(name: String) : ReleaseChannel(name)

    companion object {
        fun parse(raw: String): ReleaseChannel = when (raw) {
            STABLE.name  -> STABLE
            PREVIEW.name -> PREVIEW
            LTS.name     -> LTS
            else         -> UNKNOWN(raw)
        }
    }
}

sealed class Language(val code: String, val fullCode: String) {
    object EN : Language("en", "en-US")
    object ZH : Language("zh", "zh-CN")

    companion object {
        fun parse(raw: String): Language = when (raw) {
            EN.code, EN.fullCode -> EN
            ZH.code, ZH.fullCode -> ZH
            else                 -> throw IllegalStateException("Unsupported language $raw")
        }

        val ALL = listOf(EN, ZH)
    }
}