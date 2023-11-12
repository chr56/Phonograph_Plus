/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.IMReleaseMarkdown
import util.phonograph.releasenote.ReleaseNote
import java.io.File
import java.net.URLEncoder

fun generateEncodedUrl(model: ReleaseNote, path: String) {
    val markdown = IMReleaseMarkdown(model).write()

    val url = URLEncoder.encode(markdown, "UTF-8")

    val target = File(path)
    target.outputStream().use { output ->
        output.write(url.toByteArray())
        output.flush()
    }
}
