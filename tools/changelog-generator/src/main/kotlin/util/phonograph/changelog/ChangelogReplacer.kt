/*
 *  Copyright (c) 2023 chr_56
 */

package util.phonograph.changelog

import java.io.File

private const val TAG_LATEST = "<<<LATEST/>>>"
private const val TAG_PREVIEW_START = "<<<CURRENT_PREVIEW>>>"
private const val TAG_PREVIEW_END = "<<</CURRENT_PREVIEW>>>"


private const val ANCHOR_LATEST = "<!-- $TAG_LATEST -->"
private const val ANCHOR_PREVIEW_START = "<!-- $TAG_PREVIEW_START -->"
private const val ANCHOR_PREVIEW_END = "<!-- $TAG_PREVIEW_END -->"


private const val FILE_CHANGELOG_DEFAULT = "changelog.html"
private const val FILE_CHANGELOG_ZH = "changelog-ZH-CN.html"


internal fun String.insertLatestChangelog(item: String): String =
    this.replace(ANCHOR_LATEST, "$ANCHOR_LATEST\n\n$item")

private fun splitChangelog(fullChangelog: String): List<String> {
    val pre = fullChangelog.split(ANCHOR_PREVIEW_START, ignoreCase = true, limit = 2)
    require(pre.size == 2) { "Failed to split changelog" }
    val rest = pre[1].split(ANCHOR_PREVIEW_END, ignoreCase = true, limit = 2)
    require(rest.size == 2) { "Failed to split changelog" }
    return listOf(pre[0], rest[0], rest[1])
}

internal fun String.updatePreviewChangelog(item: String): String {
    val segments = splitChangelog(this)
    return buildString {
        append(segments[0])
        append(ANCHOR_PREVIEW_START).append('\n')
        append(item).append('\n')
        append(ANCHOR_PREVIEW_END)
        append(segments[2])
    }
}

internal fun String.clearPreviewChangelog(): String = updatePreviewChangelog("<!--  -->")


private inline fun updateFile(file: File, block: (String) -> String) {
    require(file.exists() && file.isFile)
    val fullChangelog = file.readText()
    val newText = block(fullChangelog)
    file.outputStream().use { fileOutputStream ->
        fileOutputStream.writer().use {
            it.write(newText)
            it.flush()
        }
    }
}

fun updateStableChangelog(changelog: File, lang: Language, releaseNote: Map<Language, String>) {
    updateFile(changelog) {
        val newItem = releaseNote[lang]
        require(newItem != null) { "changelog $lang is empty!" }
        it.clearPreviewChangelog().insertLatestChangelog(newItem)
    }
}

fun updatePreviewChangelog(changelog: File, lang: Language, releaseNote: Map<Language, String>) {
    updateFile(changelog) {
        val newItem = releaseNote[lang]
        require(newItem != null) { "changelog $lang is empty!" }
        it.updatePreviewChangelog(newItem)
    }
}

fun updateChangelogs(model: ReleaseNoteModel, changelogsDir: File) {
    require(changelogsDir.exists() && changelogsDir.isDirectory)

    val en = File(changelogsDir, FILE_CHANGELOG_DEFAULT)
    val zh = File(changelogsDir, FILE_CHANGELOG_ZH)

    listOf(en to Language.English, zh to Language.Chinese).forEach { (file, lang) ->
        val map = generateHTML(model)
        when (model.channel) {
            ReleaseChannel.PREVIEW -> updatePreviewChangelog(file, lang, map)
            ReleaseChannel.STABLE  -> updateStableChangelog(file, lang, map)
            ReleaseChannel.LTS     -> updateStableChangelog(file, lang, map)
            else                   -> throw Exception("Unknown channel ${model.channel}")
        }
    }

}