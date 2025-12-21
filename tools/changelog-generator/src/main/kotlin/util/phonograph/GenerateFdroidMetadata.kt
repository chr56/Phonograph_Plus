/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph

import util.phonograph.model.Language
import util.phonograph.model.ReleaseMetadata
import util.phonograph.output.FdroidChangelogTextOutput
import util.phonograph.output.FdroidMetadataVersionInfoOutput
import util.phonograph.utils.writeToFile
import java.io.File


private const val FDROID_METADATA_VERSION_INFO = "fdroid.properties"

fun generateFdroidMetadata(model: ReleaseMetadata, rootPath: String) {
    println("Processing...")
    for (lang in Language.entries) {
        val output = FdroidChangelogTextOutput(model, lang)
        val targetFile = targetFile(rootPath, lang, model.versionCode)
        println("> Processing For ${lang.displayName}...")
        writeToFile(output.write(), targetFile)
    }
    val metadata = FdroidMetadataVersionInfoOutput(model).write()
    writeToFile(metadata, File(rootPath, FDROID_METADATA_VERSION_INFO).path)
}

private fun targetFile(rootPath: String, lang: Language, versionCode: Int): File {
    val directory = File(targetPath(rootPath, lang))
    if (directory.exists()) {
        if (!directory.isDirectory) throw Exception("${directory.path} not a Directory")
    } else {
        directory.mkdirs()
    }
    val file = File(directory, "$versionCode.txt")
    if (file.exists()) {
        file.delete()
    } else {
        file.createNewFile()
    }
    return file
}

private fun targetPath(rootPath: String, lang: Language) =
    "$rootPath/fastlane/metadata/android/${lang.fullCode}/changelogs"
