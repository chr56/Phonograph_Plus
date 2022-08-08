/*
 * Copyright (c) 2022 chr_56
 */

package version.management

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import version.management.Util.currentTimeString

open class CopyArtifactsTask @Inject constructor(
    private val config: Config
) : DefaultTask() {

    class Config(
        val variantName: String,
        val isRelease: Boolean,
        val appName: String,
        val versionName: String,
        val gitHash: String = "N/A",
        val artifactsFiles: List<File>
    )

    private val productsDirectory = File(project.rootDir, "products")
    private val variantDirectory = File(productsDirectory, config.variantName)

    // the apk name to output
    private val apkName =
        if (config.isRelease) {
            "${config.appName}_${config.versionName}"
        } else {
            "${ config.appName}_${config.versionName}_${config.gitHash}_$currentTimeString"
        }

    private fun copyFiles(files: List<File>) {
        for (file in files) {
            if (!file.exists()) {
                println("${file.canonicalPath} is missing!")
                continue
            }
            file.apply {
                when {
                    name.endsWith("apk") -> {
                        copyTo(File(variantDirectory, "$apkName.apk"), true)
                    }
                    nameWithoutExtension.contains("mapping") -> {
                        copyTo(
                            File(variantDirectory, "${apkName}_mapping_${config.gitHash}.txt"),
                            true
                        )
                    }
                }
            }
        }
    }

    @TaskAction
    fun copy() {
        copyFiles(config.artifactsFiles)
        println(
            config.artifactsFiles
                .fold("Artifacts Files to copy:") { acc: String, file: File -> "$acc\n${file.absolutePath}" }
        )
    }
}
