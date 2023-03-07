/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.generateVersionJsonItem
import util.phonograph.changelog.parseReleaseNote


fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]

    val model = parseReleaseNote("$rootPath/$sourcePath")

    val item = generateVersionJsonItem(model, "preview")
    println(item)
}