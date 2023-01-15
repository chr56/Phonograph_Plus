/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class CopyArtifactsTask @Inject constructor(
    private val config: Config
) : DefaultTask() {

    class Config(
        val variantName: String,
        val isRelease: Boolean,
        val appName: String,
        val versionName: String,
        val gitHash: String = "NA",
        val artifactsFiles: List<File>
    )

    private val productsDirectory = File(project.rootDir, "products")
    private val variantDirectory = File(productsDirectory, config.variantName)

    // the apk name to output
    private val apkName =
        if (config.isRelease) {
            "${config.appName}_${config.versionName}"
        } else {
            "${config.appName}_${config.versionName}_${config.gitHash}_$currentTimeString"
        }

    private fun copyFiles(files: List<File>) {
        for (file in files) {
            copyFile(file)
        }
    }

    /**
     * copy apk and mapping only
     */
    fun copyFile(file: File) {
        if (file.exists()) {
            val name = file.nameWithoutExtension
            val extension = file.extension.lowercase()
            val destination = when {
                name.contains("mapping") -> "${apkName}_mapping_${config.gitHash}.txt"
                extension == "apk"       -> "$apkName.apk"
                else                     -> return
            }
            val target = File(variantDirectory, destination)
            file.copyTo(target, true)
        } else {
            println("${file.canonicalPath} is missing!")
            return
        }
    }

    @TaskAction
    fun copy() {
        copyFiles(config.artifactsFiles)
        println(
            config.artifactsFiles.fold("Artifacts Files to copy:")
            { acc: String, file: File -> "$acc\n${file.absolutePath}" }
        )
    }
}
