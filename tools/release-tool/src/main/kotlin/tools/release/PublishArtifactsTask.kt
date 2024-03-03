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
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import tools.release.filecopy.ApkInfo
import tools.release.filecopy.copyApk
import tools.release.filecopy.zipAndCopyMapping
import tools.release.git.getGitHash
import tools.release.text.shiftFirstLetter
import java.io.File
import javax.inject.Inject

open class PublishArtifactsTask @Inject constructor(
    private val appName: String,
    private val variant: ApplicationVariant,
) : DefaultTask() {

    init {
        description = "Publish Artifacts to target directory"
    }

    private val variantCanonicalName: String = variant.name.shiftFirstLetter()

    private val appVersionName: Provider<String?> get() = variant.outputs.first().versionName

    @TaskAction
    fun publish() {
        collect()
    }

    private fun collect() {
        val loader: BuiltArtifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        val apkDirectory: Directory = variant.artifacts.get(SingleArtifact.APK).get()
        val mappingFile = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE).get().asFile

        val builtApkArtifacts: BuiltArtifacts? = loader.load(apkDirectory)
        if (builtApkArtifacts == null) {
            println("No apks generated!")
            return
        }

        val apks: Collection<BuiltArtifact> = builtApkArtifacts.elements
        collectApksImpl(apks)

        collectMappingImpl(mappingFile)
    }


    private fun collectApksImpl(apks: Collection<BuiltArtifact>?) {
        if (!apks.isNullOrEmpty()) {
            for (apk in apks) {
                val info = ApkInfo(
                    variantName = variantCanonicalName,
                    appName = appName,
                    version = apk.versionName ?: appVersionName.orNull ?: "NA",
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
                version = appVersionName.orNull ?: "NA",
                gitHash = project.getGitHash(true),
                releaseMode = variant.buildType == "release"
            )
            zipAndCopyMapping(mappingFile, info, project.productDir())
        } else {
            println("No mapping files generated!")
        }
    }

    companion object {
        const val PRODUCTS_DIR = "products"
        internal fun Project.productDir() = File(rootDir, PRODUCTS_DIR).also { it.mkdirs() }
    }
}