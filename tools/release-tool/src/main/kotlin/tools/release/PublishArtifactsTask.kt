/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.BuiltArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import tools.release.filecopy.ApkInfo
import tools.release.filecopy.copyApk
import tools.release.filecopy.copyMapping
import tools.release.git.getGitHash
import tools.release.text.shiftFirstLetter
import java.io.File
import javax.inject.Inject

open class PublishArtifactsTask @Inject constructor(
    private val appName: String,
    private val appVersionName: String,
    private val variant: ApplicationVariant
) : DefaultTask() {

    init {
        description = "Publish Artifacts to target directory"
    }

    private val variantCanonicalName: String =
        variant.name.shiftFirstLetter()

    private val apksDirectoryProvider: Provider<Directory> =
        variant.artifacts.get(SingleArtifact.APK)
    private val mappingFileProvider: Provider<RegularFile> =
        variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)


    @TaskAction
    fun publish() {
        collectApks()
        collectMappingFile()
    }


    private fun collectApks() {
        val loader = variant.artifacts.getBuiltArtifactsLoader()
        val apkDirectory = apksDirectoryProvider.get()
        val apks = loader.load(apkDirectory)?.let { builtArtifacts ->
            builtArtifacts.elements
        }
        collectApksImpl(apks)
    }

    private fun collectMappingFile() {
        val mappingFile =
            mappingFileProvider.orNull?.asFile
        collectMappingImpl(mappingFile)
    }

    private fun collectApksImpl(apks: Collection<BuiltArtifact>?) {
        if (!apks.isNullOrEmpty()) {
            for (apk in apks) {
                val info = ApkInfo(
                    variantName = variantCanonicalName,
                    appName = appName,
                    version = apk.versionName ?: appVersionName,
                    gitHash = project.getGitHash(true),
                    releaseMode = variant.buildType == "release"
                )
                copyApk(apk, info, project.productDir())
            }
        } else {
            println("No apks generated!")
        }
    }

    private fun collectMappingImpl(mappingFile: File?) {
        if (mappingFile != null) {
            val info = ApkInfo(
                variantName = variantCanonicalName,
                appName = appName,
                version = appVersionName,
                gitHash = project.getGitHash(true),
                releaseMode = variant.buildType == "release"
            )
            copyMapping(mappingFile, info, project.productDir())
        } else {
            println("No mapping files generated!")
        }
    }

    companion object {
        internal fun Project.productDir() = File(rootDir, "products")
    }
}