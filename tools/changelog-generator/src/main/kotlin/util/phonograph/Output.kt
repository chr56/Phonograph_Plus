/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import java.io.BufferedWriter
import java.io.File

private const val Location = "products"
private const val FileGitHubReleaseMarkDown = "GitHubReleaseMarkDown.md"

const val GitHubReleaseMarkDownPath = "$Location/$FileGitHubReleaseMarkDown"

fun exportGitHubReleaseMarkDown(string: String, path: String = GitHubReleaseMarkDownPath) {
    File(path).bufferedWriter().use {
        it.write(string)
    }
}

private fun File.bufferedWriter(): BufferedWriter =
    outputStream().writer(Charsets.UTF_8).buffered(4096)