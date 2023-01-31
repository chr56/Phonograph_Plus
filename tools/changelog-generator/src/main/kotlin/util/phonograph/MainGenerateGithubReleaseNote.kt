/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.exportGitHubReleaseMarkDown
import util.phonograph.changelog.generateGitHubReleaseMarkDown
import util.phonograph.changelog.parse

fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val targetPath = args[2]

    val model = parse("$rootPath/$sourcePath")

    val markdown = generateGitHubReleaseMarkDown(model)
    exportGitHubReleaseMarkDown(markdown, "$rootPath/$targetPath")

    println(markdown)
}