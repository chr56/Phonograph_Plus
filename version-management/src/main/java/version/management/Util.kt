/*
 * Copyright (c) 2022 chr_56
 */

package version.management

import org.gradle.api.Project
import java.io.ByteArrayOutputStream

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
}
