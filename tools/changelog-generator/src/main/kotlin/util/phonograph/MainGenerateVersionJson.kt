/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.parseReleaseNote
import util.phonograph.format.generateVersionJson


fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]

    val model = parseReleaseNote("$rootPath/$sourcePath")

    val item = generateVersionJson(model, "preview")
    println(item)
}