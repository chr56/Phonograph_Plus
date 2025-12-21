/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.html

import util.phonograph.model.Language
import util.phonograph.model.ReleaseMetadata
import kotlin.collections.iterator
import java.io.File

private const val FILE_CHANGELOG_DEFAULT = "changelog.html"
private const val FILE_CHANGELOG_ZH = "changelog-ZH-CN.html"


fun updateChangelogs(model: ReleaseMetadata, changelogsDir: File) {
    require(changelogsDir.exists() && changelogsDir.isDirectory)

    val notes = generateHTML(model)

    for ((lang, note) in notes) {
        if (model.channel.isPreview) {
            updatePreviewChangelog(changelogsDir, lang, note)
        } else {
            updateStableChangelog(changelogsDir, lang, note)
        }
    }

}

fun updateStableChangelog(changelogsDir: File, lang: Language, releaseNote: String) {
    updateChangelog(changelogFile(changelogsDir, lang)) { changelogHTML ->
        changelogHTML.clearPreviewChangelog()
        changelogHTML.insertLatestChangelog(releaseNote.lines())
    }
}

fun updatePreviewChangelog(changelogsDir: File, lang: Language, releaseNote: String) {
    updateChangelog(changelogFile(changelogsDir, lang)) { changelogHTML ->
        changelogHTML.insertPreviewChangelog(releaseNote.lines())
    }
}

private inline fun updateChangelog(file: File, block: (ChangelogsHTML) -> Unit) {
    require(file.exists() && file.isFile)
    val fullChangelog = file.readText()
    val html = ChangelogsHTML.parse(fullChangelog)
    block(html)
    file.outputStream().use { fileOutputStream ->
        fileOutputStream.writer().use {
            it.write(html.output())
            it.flush()
        }
    }
}

fun changelogFile(changelogsDir: File, lang: Language): File =
    when (lang) {
        Language.EN -> File(changelogsDir, FILE_CHANGELOG_DEFAULT)
        Language.ZH -> File(changelogsDir, FILE_CHANGELOG_ZH)
    }
