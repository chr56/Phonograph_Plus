/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph

import util.phonograph.model.ReleaseMetadata
import util.phonograph.output.VersionJsonOutput
import util.phonograph.utils.writeToFile
import java.io.File


fun generateVersionJson(model: ReleaseMetadata, versionJsonFilePath: String) {
    val versionJsonFile = File(versionJsonFilePath)
    val output: String = VersionJsonOutput(versionJsonFile, model).write()
    writeToFile(output, versionJsonFile)
}