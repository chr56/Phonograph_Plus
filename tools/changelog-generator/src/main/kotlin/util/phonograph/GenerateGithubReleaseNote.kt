/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.GitHubReleaseMarkdown
import util.phonograph.releasenote.ReleaseNote

fun generateGithubReleaseNote(model: ReleaseNote, target: String) {
    val markdown = GitHubReleaseMarkdown(model).write()
    writeToFile(markdown, target)
    println(markdown)
}