/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.VersionJsonOutput
import util.phonograph.releasenote.ReleaseNote
import util.phonograph.releasenote.parseReleaseNoteYaml
import java.io.File


fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val outputPath = args[2]

    val model = parseReleaseNoteYaml(File("$rootPath/$sourcePath"))

    generateVersionJson(model, "$rootPath/$outputPath")

}

fun generateVersionJson(model: ReleaseNote, versionJsonFilePath: String) {
    val versionJsonFile = File(versionJsonFilePath)
    val output: String = VersionJsonOutput(versionJsonFile, model).write()
    writeToFile(output, versionJsonFile)
}