/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph

import util.phonograph.model.ReleaseMetadata
import util.phonograph.output.IMReleaseMarkdown
import java.io.File
import java.net.URLEncoder

fun generateEncodedUrl(model: ReleaseMetadata, path: String) {
    val markdown = IMReleaseMarkdown(model).write()

    val url = URLEncoder.encode(markdown, "UTF-8")

    val target = File(path)
    target.outputStream().use { output ->
        output.write(url.toByteArray())
        output.flush()
    }
}
