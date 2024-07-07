/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.EscapedMarkdown
import util.phonograph.releasenote.ReleaseNote
import java.io.File

fun generateEscapedMarkdownReleaseNote(model: ReleaseNote, path: String) {
    val markdown = EscapedMarkdown(model).write()

    val target = File(path)
    target.outputStream().writer().use { output ->
        output.write(markdown)
        output.flush()
    }
}
