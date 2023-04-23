/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.parseReleaseNote
import util.phonograph.changelog.updateChangelogs
import java.io.File

fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val changelogsPath = args[2]

    val model = parseReleaseNote("$rootPath/$sourcePath")

    updateChangelogs(model, File("$rootPath/$changelogsPath"))
}