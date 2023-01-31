/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

data class ReleaseNoteModel(
    val version: String,
    val time: Long,
    val note: Note
) {
    data class Note(
        val en: List<String>,
        val zh: List<String>
    )
}