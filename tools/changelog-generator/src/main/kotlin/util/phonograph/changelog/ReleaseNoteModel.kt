/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import util.phonograph.format.dateString

data class ReleaseNoteModel(
    val version: String,
    val versionCode: Int,
    val timestamp: Timestamp,
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
    object English : Language("en", "en-US")
    object Chinese : Language("zh", "zh-CN")

    companion object {
        fun parse(raw: String): Language = when (raw) {
            English.code, English.fullCode -> English
            Chinese.code, Chinese.fullCode -> Chinese
            else                           -> throw IllegalStateException("Unsupported language $raw")
        }

        val ALL = listOf(English, Chinese)
    }
}

@JvmInline
value class Timestamp(val posixTimestamp: Long) {
    val date: String get() = dateString(posixTimestamp)
}