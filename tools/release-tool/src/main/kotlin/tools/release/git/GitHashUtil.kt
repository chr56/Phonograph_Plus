/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release.git

import org.gradle.api.Project
import java.io.ByteArrayOutputStream

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
        stdout
    }.toString().trim()
