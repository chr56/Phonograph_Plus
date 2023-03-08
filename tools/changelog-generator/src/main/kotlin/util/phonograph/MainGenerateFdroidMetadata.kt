/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.parseReleaseNote
import util.phonograph.changelog.writeFdroidMetadataChangelogText

fun main(args: Array<String>) {
    val rootPath = args[0]
    val sourcePath = args[1]

    println("Parse data...")
    val model = parseReleaseNote("$rootPath/$sourcePath")

    println("Processing...")
    val languages = listOf("en-US", "zh-CN")
    for (lang in languages) {
        writeFdroidMetadataChangelogText(model, rootPath, lang)
    }
    println("Completed!")
}