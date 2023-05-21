/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

data class ReleaseNoteModel(
    val version: String,
    val versionCode: Int,
    val time: Long,
    val channel: ReleaseChannel?,
    val note: Note
) {
    data class Note(
        val en: List<String>,
        val zh: List<String>
    )
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
            "en","en-US" -> EN
            "zh","zh-CN" -> ZH
            else -> throw IllegalStateException("Unsupported language $raw")
        }
    }
}