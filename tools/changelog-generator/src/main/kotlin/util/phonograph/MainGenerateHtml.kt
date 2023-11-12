/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.html.generateHTML
import util.phonograph.releasenote.parseReleaseNoteYaml
import java.io.File

fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]

    val model = parseReleaseNoteYaml(File("$rootPath/$sourcePath"))

    val html = generateHTML(model)
    for ((lang, text) in html) {
        println("lang $lang:")
        println(text)
    }
}