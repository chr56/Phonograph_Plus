/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import util.phonograph.format.dateString
import java.io.File

fun generateFdroidMetadataChangelogText(model: ReleaseNoteModel, lang: String): String =
    buildString {
        appendLine("${model.version}(${model.versionCode}) ${dateString(model.time)}").append('\n')
        val note = when (lang) {
            "en-US" -> model.note.en
            "zh-CN" -> model.note.zh
            else    -> throw IllegalArgumentException("illegal language $lang")
        }
        for (line in note) {
            appendLine("- $line")
        }
    }

fun writeFdroidMetadataChangelogText(model: ReleaseNoteModel, rootPath: String, lang: String) {
    val text = generateFdroidMetadataChangelogText(model, lang)
    val file = targetFile(rootPath, lang, model.versionCode)
    writeToFile(text, file)
}

private fun targetFile(rootPath: String, lang: String, versionCode: Int): File {
    val directory = File(targetPath(rootPath, lang))
    if (directory.exists()) {
        if (!directory.isDirectory) throw Exception("${directory.path} not a Directory")
    } else {
        directory.mkdirs()
    }
    val file = File(directory, "$versionCode.txt")
    if (file.exists()) {
        file.delete()
    } else {
        file.createNewFile()
    }
    return file
}

private fun targetPath(rootPath: String, lang: String) = "$rootPath/fastlane/metadata/android/$lang/changelogs"