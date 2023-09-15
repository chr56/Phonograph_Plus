/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.writeToFile
import util.phonograph.output.VersionJsonOutput
import util.phonograph.releasenote.parseReleaseNoteToml
import java.io.File


fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val outputPath = args[2]

    println("Parse data...")
    val model = parseReleaseNoteToml(File("$rootPath/$sourcePath"))
    val versionJsonFile = File("$rootPath/$outputPath")

    println("Process version json")
    val str = VersionJsonOutput(versionJsonFile, model).write()

    println("Output...")
    writeToFile(str, "$rootPath/$outputPath")

    println("Completed")
}