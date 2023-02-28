/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import java.io.File


fun parse(path: String): ReleaseNoteModel = parse(File(path))

fun parse(file: File): ReleaseNoteModel {

    val result = mutableMapOf<String, String>()

    var noteCollectTarget: String? = null
    val buffer = mutableListOf<String>()
    val note = mutableMapOf<String, List<String>>()

    val lines = file.readLines(Charsets.UTF_8)
    for ((index, line) in lines.withIndex()) {
        if (line.startsWith('#')) {
            val tags = parseMarks(line, index)
            result.putAll(tags)
            if (tags.containsKey("note")) {
                if (noteCollectTarget != null) { // close last and summit
                    note[noteCollectTarget] = buffer.toList()
                    buffer.clear()
                }
                noteCollectTarget = tags["note"]
            }
            continue
        }

        if (noteCollectTarget != null) {
            if (line.startsWith('-'))
                buffer.add(line.substringAfter("- "))
        }

    }

    if (noteCollectTarget != null) {
        note[noteCollectTarget] = buffer
    }
    return ReleaseNoteModel(
        version = result["version"] ?: "NA",
        versionCode = result["versionCode"]?.toInt() ?: -1,
        time = result["date"]?.toLongOrNull() ?: 0,
        note = ReleaseNoteModel.Note(
            note["en"] ?: emptyList(),
            note["zh"] ?: emptyList(),
        )
    )
}

fun parseMarks(str: String, lineNum: Int): Map<String, String> {
    val map = mutableMapOf<String, String>()

    var bracket = 0
    var collectMode = false
    var buffer = CharArray(str.length)
    var bufferPosition = 0
    var sepPosition = 0

    for ((index, char) in str.withIndex()) {
        if (char == '{') {
            bracket++
            if (bracket >= 2) {
                collectMode = true
            }
            continue
        } else if (char == '}') {
            if (bracket == 0)
                throw IllegalStateException("unmatched bracket (line $lineNum, pos $index)!")
            bracket--
            if (bracket == 0) {
                collectMode = false
                // summit
                if (sepPosition == 0)
                    throw IllegalStateException("No key value separator (line $lineNum)!")
                val key = buildString {
                    for (i in 0 until sepPosition) append(buffer[i])
                }
                val value = buildString {
                    for (i in sepPosition + 1 until bufferPosition) append(buffer[i])
                }
                map[key] = value
                // clear
                buffer = CharArray(str.length)
                bufferPosition = 0
                sepPosition = 0
            }
            continue
        }
        if (collectMode) {
            buffer[bufferPosition] = char
            if (char == ':') sepPosition = bufferPosition
            bufferPosition++
        }
    }

    return map
}