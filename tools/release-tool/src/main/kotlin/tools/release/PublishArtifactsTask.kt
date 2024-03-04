/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.TaskAction
import tools.release.Plugin.Companion.productDir
import tools.release.file.assureDir
import tools.release.file.hashValidationFile
import tools.release.git.getGitHash
import tools.release.text.NameStyle
import tools.release.text.canonicalName
import tools.release.zip.gzip
import java.io.File
import javax.inject.Inject

open class PublishArtifactsTask @Inject constructor(
    private val name: String,
    private val variant: ApplicationVariant,
) : DefaultTask() {

    init {
        description = "Publish Artifacts to target directory"
    }

    @TaskAction
    fun publish() {
        collect()
    }

    private fun collect() {
        val loader: BuiltArtifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        val apkDirectory: Directory = variant.artifacts.get(SingleArtifact.APK).get()
        val mappingFile: RegularFile? = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE).orNull

        val builtApkArtifacts: BuiltArtifacts? = loader.load(apkDirectory)
        if (builtApkArtifacts == null) {
            println("No apks generated!")
            return
        }

        val nameStyle =
            if (variant.buildType != "debug") {
                NameStyle.Version
            } else {
                NameStyle.VersionGitHashTime(project.getGitHash(true))
            }

        val destinationDir: File = File(project.productDir(), variant.canonicalName).assureDir()

        exportArtifact(
            variant,
            builtApkArtifacts.elements,
            mappingFile,
            name,
            nameStyle,
            destinationDir,
        )
    }

    companion object {
        private fun exportArtifact(
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
                val destination = File(destinationDir, "$apkName.apk")
                val file = File(artifact.outputFile)
                file.copyTo(destination, overwrite)
                destination.hashValidationFile("SHA-1")
                destination.hashValidationFile("SHA-256")
                notify(destination)
            }

            val mapping: File? = mappingFile?.asFile
            if (mapping != null && mapping.exists()) {
                val mappingName = nameStyle.generateMappingName(name, variant)
                val destination = File(destinationDir, "$mappingName.txt.gz")
                val file = mapping.gzip()
                file.copyTo(destination, overwrite)
                notify(destination)
            }
        }

        private fun notify(file: File) {
            val osName = System.getProperty("os.name").lowercase()
            val location =
                if (osName.contains("windows")) {
                    file.toURI().toString().replace("file:/", "file:///")
                } else {
                    file.toURI().toString()
                }
            println("Copied! $location")
        }
    }


}