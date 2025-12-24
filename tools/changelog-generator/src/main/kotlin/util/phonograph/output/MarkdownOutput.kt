/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.formater.MarkdownFormater
import util.phonograph.model.OutputFormat
import util.phonograph.model.ReleaseChannel
import util.phonograph.model.ReleaseMetadata
import util.phonograph.model.TargetVariant
import util.phonograph.model.constants.COMMIT_LOG_PREFIX
import util.phonograph.model.constants.DOWNLOAD_LINK_TEMPLATE
import util.phonograph.model.constants.PREVIEW_WARNING_EN
import util.phonograph.model.constants.PREVIEW_WARNING_ZH
import util.phonograph.model.constants.VARIANTS_DESCRIPTION_BODY
import util.phonograph.model.constants.VARIANTS_DESCRIPTION_TITLE
import util.phonograph.model.constants.compareLink
import util.phonograph.model.constants.downloadLink
import util.phonograph.utils.dateString
import java.io.Writer



sealed class ReleaseMarkdown : OutputFormat, MarkdownFormater()

class GitHubReleaseMarkdown(private val metadata: ReleaseMetadata) : ReleaseMarkdown() {

    @Suppress("SameParameterValue")
    private fun section(note: ReleaseMetadata.Notes.Note, title: String, level: Int): String = buildString {
        appendLine(title(title, level))
        if (note.notice != null) appendLine(githubAlertBox(note.notice)).append('\n')
        if (note.highlights.isNotEmpty()) appendLine(makeUnorderedList(note.highlights)).append('\n')
        if (note.items.isNotEmpty()) appendLine(makeOrderedList(note.items)).append('\n')
    }

    private fun previewWarning(channel: ReleaseChannel): String? =
        when (channel) {
            ReleaseChannel.PREVIEW -> githubAlertBox(
                "$PREVIEW_WARNING_EN\n$PREVIEW_WARNING_ZH", AlertType.Important
            )

            else                   -> null
        }

    override fun write(target: Writer) {

        // Title
        target.append(
            title(border("v${metadata.version} ${dateString(metadata.timestamp)}"), 2)
        )
        target.append('\n')

        // Warnings
        val warning = previewWarning(metadata.channel)
        if (warning != null) {
            target.append(warning)
            target.append('\n')
        }

        // Main Content
        target.append(section(metadata.notes.en, "EN", 3))
        target.append(section(metadata.notes.zh, "ZH", 3))
        target.append('\n')


        // Diff Link
        target.append("$COMMIT_LOG_PREFIX ${compareLink(metadata.previousTag, metadata.tag)}").append('\n')
        target.append('\n')

        // Variants Description
        target.append(title(VARIANTS_DESCRIPTION_TITLE, 2)).append('\n')
        target.append(
            VARIANTS_DESCRIPTION_BODY.trimIndent()
        ).append('\n')
        target.append('\n')

        // Download Links
        val downloadLinkModern =
            downloadLink(metadata.tag, metadata.version, metadata.variant(TargetVariant.MODERN))
        val downloadLinkLegacy =
            downloadLink(metadata.tag, metadata.version, metadata.variant(TargetVariant.LEGACY))
        target.append(
            String.format(
                DOWNLOAD_LINK_TEMPLATE,
                downloadLinkModern,
                downloadLinkLegacy
            ).trimIndent()
        )
        target.append('\n')
    }
}

class EscapedMarkdown(private val metadata: ReleaseMetadata) : ReleaseMarkdown() {

    override fun border(text: String): String = "*$text*"

    private fun section(note: ReleaseMetadata.Notes.Note, title: String): String = buildString {
        appendLine(border(title))
        if (note.notice != null) appendLine(escapeMarkdown(note.notice)).append('\n')
        if (note.highlights.isNotEmpty()) appendLine(escapeMarkdown(makeOrderedList(note.highlights))).append('\n')
        if (note.items.isNotEmpty()) appendLine(escapeMarkdown(makeOrderedList(note.items)))
    }

    private fun previewWarning(channel: ReleaseChannel): String? =
        when (channel) {
            ReleaseChannel.PREVIEW -> buildString {
                append(escapeMarkdownV2(PREVIEW_WARNING_EN)).append('\n')
                append(escapeMarkdownV2(PREVIEW_WARNING_ZH)).append('\n')
            }
            else                   -> null
        }

    override fun write(target: Writer) {
        // Title
        target.append(
            border(escapeMarkdown("${(metadata.tag)} ${dateString(metadata.timestamp)}"))
        )
        target.append('\n').append('\n')
        // Warning
        val warning = previewWarning(metadata.channel)
        if (warning != null) {
            target.append(warning)
            target.append('\n')
        }
        // Content
        target.append(section(metadata.notes.en, "EN"))
        target.append(section(metadata.notes.zh, "ZH"))
    }
}