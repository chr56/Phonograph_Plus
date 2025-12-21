/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.model.Language
import util.phonograph.model.OutputFormat
import util.phonograph.model.ReleaseMetadata
import util.phonograph.model.constants.OVERFLOWED_MESSAGE
import util.phonograph.utils.dateString
import java.io.Writer


class FdroidChangelogTextOutput(val metadata: ReleaseMetadata, val language: Language) : OutputFormat {

    override fun write(target: Writer) {
        val changelogText = generateFdroidMetadataChangelogText(metadata, language)
        target.write(changelogText)
    }

    private fun generateFdroidMetadataChangelogText(model: ReleaseMetadata, language: Language): String =
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


