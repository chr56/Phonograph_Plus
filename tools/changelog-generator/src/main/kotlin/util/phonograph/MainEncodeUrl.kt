/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.IMReleaseMarkdown
import util.phonograph.releasenote.ReleaseNote
import util.phonograph.releasenote.parseReleaseNoteYaml
import java.io.File
import java.net.URLEncoder


fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val targetPath = args[2]

    val model = parseReleaseNoteYaml(File("$rootPath/$sourcePath"))

    generateEncodedUrl(model, "$rootPath/$targetPath")
}

fun generateEncodedUrl(model: ReleaseNote, path: String) {
    val markdown = IMReleaseMarkdown(model).write()

    val url = URLEncoder.encode(markdown, "UTF-8")

    val target = File(path)
    target.outputStream().use { output ->
        output.write(url.toByteArray())
        output.flush()
    }
}
