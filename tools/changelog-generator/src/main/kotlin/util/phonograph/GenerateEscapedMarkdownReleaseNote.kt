/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph

import util.phonograph.model.ReleaseMetadata
import util.phonograph.output.EscapedMarkdown
import java.io.File

fun generateEscapedMarkdownReleaseNote(model: ReleaseMetadata, path: String) {
    val markdown = EscapedMarkdown(model).write()

    val target = File(path)
    target.outputStream().writer().use { output ->
        output.write(markdown)
        output.flush()
    }
}
