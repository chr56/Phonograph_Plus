/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.dateString
import util.phonograph.escapeMarkdown
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.Notes
import util.phonograph.releasenote.ReleaseChannel
import util.phonograph.releasenote.ReleaseNote
import java.io.Writer

abstract class Markdown : OutputFormat {

    protected open fun border(text: String) = "**$text**"

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

    protected fun subtitle(language: Language, previewWarning: Boolean, escaped: Boolean = false): String =
        buildString {
            when (language) {
                Language.EN -> {
                    if (previewWarning) appendLine(if (escaped) PREVIEW_WARNING_EN_ESCAPED else PREVIEW_WARNING_EN)
                }

                Language.ZH -> {
                    if (previewWarning) appendLine(if (escaped) PREVIEW_WARNING_ZH_ESCAPED else PREVIEW_WARNING_ZH)
                }
            }
        }

    protected fun guideOnDownload(): String = buildString {
        appendLine(title("Version Variants Description / 版本说明", 2))
        appendLine(VERSION_DESCRIPTION.trimIndent())
    }

    companion object {

        private const val PREVIEW_WARNING_EN =
            "This is a _Preview Channel_ Release (identified by `preview` suffix in the package name), stability and quality are not guaranteed."
        private const val PREVIEW_WARNING_ZH =
            "此为预览通道版本 (包名后缀`preview`), 不保证可靠性!"

        private const val PREVIEW_WARNING_EN_ESCAPED =
            "This is a _Preview Channel_ Release \\(identified by `preview` suffix in the package name\\)\\, stability and quality are not guaranteed\\."
        private const val PREVIEW_WARNING_ZH_ESCAPED =
            "此为预览通道版本 \\(包名后缀`preview`\\), 不保证可靠性\\!"

        private const val VERSION_DESCRIPTION =
            """
            -> [Version Guide](docs/Version_Guide.md) / [版本指南](docs/Version_Guide_ZH.md)
            
            **TL;DR**: If you are a user of Android 7 ~ 10, please consider to use `Legacy`; If not, use `Modern`. The `Fdroid` is identical to the version on F‑droid; and the signature is same with others.
            **太长不看**: 若为 Android 7 ~ 10 用户，请考虑使用 `Legacy` 版本；否则，请使用 `Modern` 版本。另外带 `Fdroid` 为 F-droid 上完全相同的版本，签名与其他版本相同。
            
            """
    }
}


class GitHubReleaseMarkdown(private val releaseNote: ReleaseNote) : Markdown() {

    @Suppress("SameParameterValue")
    private fun section(note: Notes.Note, title: String, level: Int): String = buildString {
        appendLine(title(title, level))
        if (note.notice != null) appendLine(githubAlertBox(note.notice)).append('\n')
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

        target.append(fileDownloadLinks(releaseNote.tag, releaseNote.version, releaseNote.channel))
    }

    companion object {

        fun fileDownloadLinks(tag: String, version: String, channel: ReleaseChannel): String =
            buildString {
                val channelText = when (channel) {
                    ReleaseChannel.PREVIEW -> "Preview"
                    else                   -> "Stable"
                }
                append("Download Links ")
                append('|')
                append(" [Modern](https://github.com/chr56/Phonograph_Plus/releases/download/${tag}/PhonographPlus_${version}_Modern${channelText}Release.apk) |")
                append(" [Legacy](https://github.com/chr56/Phonograph_Plus/releases/download/${tag}/PhonographPlus_${version}_Legacy${channelText}Release.apk) |")
            }

        private fun githubAlertBox(text: String, type: String = "NOTE"): String {
            val lines = text.lines()
            return buildString {
                append("> [!$type]")
                append('\n')
                for (line in lines) {
                    append("> ")
                    append(line)
                    append('\n')
                }
            }
        }

        @JvmStatic
        private fun generateDiffLink(releaseNote: ReleaseNote): String =
            GITHUB_DIFF.format(releaseNote.previousTag, releaseNote.tag)

        private const val GITHUB_DIFF = "https://github.com/chr56/Phonograph_Plus/compare/%s...%s"
    }
}

class EscapedMarkdown(private val releaseNote: ReleaseNote) : Markdown() {

    override fun border(text: String): String = "*$text*"

    private fun section(note: Notes.Note, title: String): String = buildString {
        appendLine(border(title))
        if (note.notice != null) appendLine(escapeMarkdown(note.notice)).append('\n')
        if (note.highlights.isNotEmpty()) appendLine(escapeMarkdown(makeOrderedList(note.highlights))).append('\n')
        if (note.items.isNotEmpty()) appendLine(escapeMarkdown(makeOrderedList(note.items)))
    }

    override fun write(target: Writer) {


        val title = border(escapeMarkdown("${(releaseNote.tag)} ${dateString(releaseNote.timestamp)}"))

        val subtitleEN = subtitle(Language.EN, releaseNote.channel == ReleaseChannel.PREVIEW, true)
        val subtitleZH = subtitle(Language.ZH, releaseNote.channel == ReleaseChannel.PREVIEW, true)

        val contentEN = section(releaseNote.notes.en, "EN")
        val contentZH = section(releaseNote.notes.zh, "ZH")

        target.append(title)
        target.append('\n').append('\n')
        target.append(subtitleEN)
        target.append(subtitleZH)
        target.append('\n')
        target.append(contentEN)
        target.append(contentZH)
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