/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.VersionJsonOutput
import util.phonograph.releasenote.ReleaseNote
import java.io.File


fun generateVersionJson(model: ReleaseNote, versionJsonFilePath: String) {
    val versionJsonFile = File(versionJsonFilePath)
    val output: String = VersionJsonOutput(versionJsonFile, model).write()
    writeToFile(output, versionJsonFile)
}