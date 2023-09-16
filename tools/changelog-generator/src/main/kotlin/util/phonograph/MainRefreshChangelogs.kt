/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.html.updateChangelogs
import util.phonograph.releasenote.parseReleaseNoteToml
import java.io.File

fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val changelogsPath = args[2]

    val model = parseReleaseNoteToml(File("$rootPath/$sourcePath"))

    updateChangelogs(model, File("$rootPath/$changelogsPath"))
}