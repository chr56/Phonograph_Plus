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
    }.cutOff()

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

fun writeFdroidMetadataVersionInfo(model: ReleaseNoteModel, rootPath: String) {
    if (model.channel == "stable") {
        val text = generateFdroidMetadataVersionInfo(model)
        val file = File(rootPath, FDROID_METADATA_VERSION_INFO)
        if (file.exists()) file.delete() else file.createNewFile()
        writeToFile(text, file)
    } else {
        println(" (${model.channel} channel, ignored!)")
    }
}

private fun generateFdroidMetadataVersionInfo(model: ReleaseNoteModel): String =
    buildString {
        appendLine("$LATEST_VERSION_NAME=${model.version}")
        appendLine("$LATEST_VERSION_CODE=${model.versionCode}")
    }

private const val FDROID_METADATA_VERSION_INFO = "fdroid.properties"

private const val LATEST_VERSION_NAME = "latest_version_name"
private const val LATEST_VERSION_CODE = "latest_version_code"


/**
 * take first bytes of [text]
 * @param overflowMessage suffix message if [text] was cut off
 * @param limit size in BYTE
 */
private fun cutOffOverflowedText(text: String, overflowMessage: String = OVERFLOWED_MESSAGE, limit: Int = 500): String {
    val textBytes = text.encodeToByteArray()
    val textSize = textBytes.size
    return if (textSize > limit) {
        val overflowMessageBytes = overflowMessage.encodeToByteArray()

        // taken size of cut-offed text
        var validSize = limit - overflowMessageBytes.size - 1
        while (textBytes[validSize].toInt() < 0) {
            // make sure not cut a character longer than 2 (whose msb is not 0)
            validSize--
        }

        // remaining String
        val cutOff = ByteArray(validSize) { textBytes[it] }

        "${String(cutOff)}\n$overflowMessage"
    } else {
        text
    }
}

private fun String.cutOff(overflowMessage: String = OVERFLOWED_MESSAGE, limit: Int = 500): String =
    cutOffOverflowedText(this, overflowMessage, limit)

private const val OVERFLOWED_MESSAGE = "...(Visit project homepage to see full changelogs)"
