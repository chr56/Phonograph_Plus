/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.Language
import util.phonograph.changelog.parseReleaseNote
import util.phonograph.changelog.writeFdroidMetadataChangelogText
import util.phonograph.changelog.writeFdroidMetadataVersionInfo

fun main(args: Array<String>) {
    val rootPath = args[0]
    val sourcePath = args[1]

    println("Parse data...")
    val model = parseReleaseNote("$rootPath/$sourcePath")

    println("Processing...")
    for (lang in Language.ALL) {
        writeFdroidMetadataChangelogText(model, rootPath, lang)
    }
    writeFdroidMetadataVersionInfo(model, rootPath)
    println("Completed!")
}