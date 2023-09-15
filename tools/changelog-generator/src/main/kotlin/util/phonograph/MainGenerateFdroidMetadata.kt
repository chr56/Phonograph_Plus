/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.writeFdroidMetadataChangelogText
import util.phonograph.changelog.writeFdroidMetadataVersionInfo
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.parseReleaseNoteToml
import java.io.File

fun main(args: Array<String>) {
    val rootPath = args[0]
    val sourcePath = args[1]

    println("Parse data...")
    val model = parseReleaseNoteToml(File("$rootPath/$sourcePath"))

    println("Processing...")
    for (lang in Language.ALL) {
        writeFdroidMetadataChangelogText(model, rootPath, lang)
    }
    writeFdroidMetadataVersionInfo(model, rootPath)
    println("Completed!")
}