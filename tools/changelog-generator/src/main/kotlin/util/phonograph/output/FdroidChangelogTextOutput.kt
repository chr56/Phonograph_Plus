/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.format.dateString
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.ReleaseNote
import java.io.Writer



private const val OVERFLOWED_MESSAGE = "...(Visit project homepage to see full changelogs)"

class FdroidChangelogTextOutput(val releaseNote: ReleaseNote, val language: Language) : OutputFormat {

    override fun write(target: Writer) {
        val changelogText = generateFdroidMetadataChangelogText(releaseNote, language)
        target.write(changelogText)
    }

    private fun generateFdroidMetadataChangelogText(model: ReleaseNote, language: Language): String =
        buildString {
            appendLine("<b>${model.version}(${model.versionCode}) ${dateString(model.timestamp)}</b>")
            val lines = model.language(language).items
            for (line in lines) {
                appendLine("- $line")
            }
        }.cutOff()


    /**
     * take first bytes of [text]
     * @param overflowMessage suffix message if [text] was cut off
     * @param limit size in BYTE
     */
    private fun cutOffOverflowedText(
        text: String,
        overflowMessage: String = OVERFLOWED_MESSAGE,
        limit: Int = 500,
    ): String {
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
}


