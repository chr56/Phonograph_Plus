/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.TaskAction
import tools.release.filecopy.NameStyle
import tools.release.filecopy.assureDir
import tools.release.filecopy.exportArtifact
import tools.release.git.getGitHash
import tools.release.text.canonicalName
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
            destinationDir
        )
    }

    companion object {
        const val PRODUCTS_DIR = "products"
        internal fun Project.productDir() = File(rootDir, PRODUCTS_DIR).also { it.mkdirs() }
    }
}