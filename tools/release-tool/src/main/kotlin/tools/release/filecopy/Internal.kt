/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release.filecopy

import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.FilterConfiguration
import org.gradle.api.file.RegularFile
import tools.release.text.currentTimeString
import tools.release.zip.gzip
import java.io.File

sealed interface NameStyle {

    fun generateApkName(
        name: String,
        variant: ApplicationVariant,
        artifact: BuiltArtifact,
    ): String



    fun generateMappingName(
        name: String,
        variant: ApplicationVariant,
    ): String


    data object Version : NameStyle {
        override fun generateApkName(name: String, variant: ApplicationVariant, artifact: BuiltArtifact): String {
            val version = artifact.versionName ?: "NA"
            return "${name}_${version}"
        }

        override fun generateMappingName(name: String, variant: ApplicationVariant): String {
            return "mapping_${name}"
        }
    }

    data object VersionAbi : NameStyle {
        override fun generateApkName(name: String, variant: ApplicationVariant, artifact: BuiltArtifact): String {
            val version = artifact.versionName ?: "NA"
            val abiList =
                artifact.filters
                    .filter { it.filterType == FilterConfiguration.FilterType.ABI }
                    .takeIf { it.isNotEmpty() }
            val abi = abiList?.joinToString(separator = "-") ?: "universal"
            return "${name}_${version}_${abi}"
        }

        override fun generateMappingName(name: String, variant: ApplicationVariant): String {
            return "mapping_${name}"
        }
    }

    class VersionGitHashTime(val gitHash: String) : NameStyle {
        override fun generateApkName(name: String, variant: ApplicationVariant, artifact: BuiltArtifact): String {
            val version = artifact.versionName ?: "NA"
            return "${name}_${version}_${gitHash}_${currentTimeString}"
        }

        override fun generateMappingName(name: String, variant: ApplicationVariant): String {
            return "mapping_${name}_${gitHash}"
        }
    }
}

internal fun exportArtifact(
    variant: ApplicationVariant,
    artifacts: Collection<BuiltArtifact>,
    mappingFile: RegularFile?,
    name: String,
    nameStyle: NameStyle,
    destinationDir: File,
    overwrite: Boolean = true,
) {

    for (artifact in artifacts) {
        val apkName = nameStyle.generateApkName(name, variant, artifact)
        copyFile(File(artifact.outputFile), destinationDir, "$apkName.apk", overwrite)
        println("copied! apk     file:///$destinationDir/$apkName.apk")
    }

    val mapping: File? = mappingFile?.asFile
    if (mapping != null && mapping.exists()) {
        val mappingName = nameStyle.generateMappingName(name, variant)
        copyFile(mapping.gzip(), destinationDir, "$mappingName.txt.gz", overwrite)
        println("copied! mapping file:///$destinationDir/$mappingName.txt.gz")
    }
}


private fun copyFile(
    file: File,
    destinationDir: File,
    newName: String,
    overwrite: Boolean = true,
) {
    file.copyTo(File(destinationDir, newName), overwrite)
}

fun File.assureDir(): File {
    require(isDirectory) { "$name not a directory!" }
    if (!exists()) mkdirs()
    return this
}