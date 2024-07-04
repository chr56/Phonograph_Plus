/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.dateString
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.Notes
import util.phonograph.releasenote.ReleaseChannel
import util.phonograph.releasenote.ReleaseNote
import java.io.Writer

abstract class Markdown : OutputFormat {

    protected fun border(text: String) = "**$text**"

    protected fun title(text: String, level: Int) = "${"#".repeat(level)} $text\n"

    protected fun makeUnorderedList(items: List<String>) = buildString {
        for (item in items) {
            appendLine("- $item")
        }
    }

    protected fun makeOrderedList(items: List<String>) = buildString {
        for ((index, item) in items.withIndex()) {
            appendLine("${index + 1}. $item")
        }
    }

    protected fun subtitle(language: Language, previewWarning: Boolean): String = buildString {
        when (language) {
            Language.EN -> {
                if (previewWarning) appendLine("This is a _Preview Channel_ Release (with package name suffix `preview`), which might have potential bugs.")
            }

            Language.ZH -> {
                if (previewWarning) appendLine("此为预览通道版本 (包名后缀`preview`), 可能存在潜在问题!")
            }
        }
    }

    protected fun guideOnDownload(): String = buildString {
        appendLine(title("Version Variants Description / 版本说明", 2))
        appendLine(VERSION_DESCRIPTION.trimIndent())
    }

    companion object {

        private const val VERSION_DESCRIPTION =
            """
            -> [Version Guide](docs/Version_Guide.md) / [版本指南](docs/Version_Guide_ZH.md)
            
            **TL;DR**: If you are a user of Android 7-10, use `Legacy`; If not, use `Modern`.
            **太长不看**: 若为 Android 7-10 用户，请使用 `Legacy` 版本；否则，请使用 `Modern` 版本。
            
            """
    }
}


class GitHubReleaseMarkdown(private val releaseNote: ReleaseNote) : Markdown() {

    @Suppress("SameParameterValue")
    private fun section(note: Notes.Note, title: String, level: Int): String = buildString {
        appendLine(title(title, level))
        if (note.highlights.isNotEmpty()) appendLine(makeUnorderedList(note.highlights)).append('\n')
        if (note.items.isNotEmpty()) appendLine(makeOrderedList(note.items)).append('\n')
    }


    override fun write(target: Writer) {

        val title = title(border("v${releaseNote.version} ${dateString(releaseNote.timestamp)}"), 2)

        val subtitleEN = subtitle(Language.EN, releaseNote.channel == ReleaseChannel.PREVIEW)
        val subtitleZH = subtitle(Language.ZH, releaseNote.channel == ReleaseChannel.PREVIEW)
        val contentEN = section(releaseNote.notes.en, "EN", 3)
        val contentZH = section(releaseNote.notes.zh, "ZH", 3)

        target.append(title)
        target.append('\n')
        target.append(subtitleEN)
        target.append(subtitleZH)
        target.append('\n')
        target.append(contentEN)
        target.append(contentZH)
        target.append('\n')


        val diff = "**Commit log**: ${generateDiffLink(releaseNote)}"
        target.append(diff)

        target.append('\n').append('\n')
        target.append(guideOnDownload())
    }

    companion object {

        @JvmStatic
        private fun generateDiffLink(releaseNote: ReleaseNote): String =
            GITHUB_DIFF.format(releaseNote.previousTag, releaseNote.tag)

        private const val GITHUB_DIFF = "https://github.com/chr56/Phonograph_Plus/compare/%s...%s"
    }
}

class IMReleaseMarkdown(private val releaseNote: ReleaseNote) : Markdown() {

    private fun section(note: Notes.Note, title: String): String = buildString {
        appendLine(border(title)).append('\n')
        if (note.highlights.isNotEmpty()) appendLine(makeOrderedList(note.highlights)).append('\n')
        if (note.items.isNotEmpty()) appendLine(makeOrderedList(note.items)).append('\n')
    }

    override fun write(target: Writer) {

        val title = border("v${releaseNote.version} ${dateString(releaseNote.timestamp)}")
        val en = section(releaseNote.notes.en, "EN")
        val zh = section(releaseNote.notes.zh, "ZH")

        target.append(title).append('\n').append('\n')
        target.append(en).append('\n')
        target.append(zh).append('\n')

    }
}