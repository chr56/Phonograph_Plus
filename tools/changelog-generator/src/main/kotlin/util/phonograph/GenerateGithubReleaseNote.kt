/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph

import util.phonograph.model.ReleaseMetadata
import util.phonograph.output.GitHubReleaseMarkdown
import util.phonograph.utils.writeToFile

fun generateGithubReleaseNote(model: ReleaseMetadata, target: String) {
    val markdown = GitHubReleaseMarkdown(model).write()
    writeToFile(markdown, target)
    println(markdown)
}