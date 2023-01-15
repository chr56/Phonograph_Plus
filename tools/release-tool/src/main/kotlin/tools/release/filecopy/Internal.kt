/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release.filecopy

import com.android.build.api.variant.BuiltArtifact
import tools.release.text.currentTimeString
import java.io.File

data class ApkInfo(
    val variantName: String,
    val appName: String,
    val version: String,
    val gitHash: String,
    val releaseMode: Boolean = false,
)

internal fun apkName(
    appName: String,
    version: String,
    gitHash: String,
    releaseMode: Boolean = false,
): String {
    return if (releaseMode) {
        "${appName}_${version}"
    } else {
        "${appName}_${version}_${gitHash}_$currentTimeString"
    }
}

internal fun copyApk(
    apkArtifact: BuiltArtifact,
    apkInfo: ApkInfo,
    productDir: File,
    overwrite: Boolean = true
) {
    val newName = apkName(apkInfo.appName, apkInfo.version, apkInfo.gitHash, apkInfo.releaseMode)
    val destinationDir = File(productDir, apkInfo.variantName)
    copyApkImpl(apkArtifact, destinationDir, newName, overwrite)
}

private fun copyApkImpl(
    artifact: BuiltArtifact,
    destinationDir: File,
    apkName: String,
    overwrite: Boolean = true
) {
    val name = "$apkName.apk"
    copyFile(File(artifact.outputFile), destinationDir, name, overwrite)
    println("copied! apk     $name")
}


internal fun copyMapping(
    mappingFile: File,
    apkInfo: ApkInfo,
    productDir: File,
    overwrite: Boolean = true
) {
    val destinationDir = File(productDir, apkInfo.variantName)
    val apkName = apkName(apkInfo.appName, apkInfo.version, apkInfo.gitHash, apkInfo.releaseMode)
    copyMappingImpl(mappingFile, destinationDir, apkName, apkInfo.gitHash, overwrite)
}

private fun copyMappingImpl(
    mappingFile: File,
    destinationDir: File,
    apkName: String,
    gitHash: String,
    overwrite: Boolean = true
) {
    val name = "${apkName}_mapping_${gitHash}.txt"
    copyFile(mappingFile, destinationDir, name, overwrite)
    println("copied! mapping $name")
}

private fun copyFile(
    file: File,
    destinationDir: File,
    newName: String,
    overwrite: Boolean = true
) {
    file.copyTo(File(destinationDir, newName), overwrite)
}