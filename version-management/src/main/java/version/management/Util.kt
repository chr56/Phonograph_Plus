/*
 * Copyright (c) 2022 chr_56
 */

package version.management

import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File

object Util {
    fun Project.getGitHash(shortHash: Boolean): String =
        ByteArrayOutputStream().use { stdout ->
            exec {
                if (shortHash) {
                    it.commandLine("git", "rev-parse", "--short", "HEAD")
                } else {
                    it.commandLine("git", "rev-parse", "HEAD")
                }
                it.standardOutput = stdout
            }
        }.toString().trim()

    fun encodeReleaseNoteToUrl(inputFile: File, outputFile: File) {
        if (inputFile.exists()) {
            java.io.FileInputStream(inputFile).use { input ->
                val content = String(input.readAllBytes())
                val result = java.net.URLEncoder.encode(content, "UTF-8")
                java.io.FileOutputStream(outputFile).use { output ->
                    output.write(result.toByteArray())
                    output.flush()
                }
            }
        }
    }
}
