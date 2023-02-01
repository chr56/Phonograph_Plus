/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.generateGitHubReleaseMarkDown
import util.phonograph.changelog.parse
import java.io.File
import java.net.URLEncoder


fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val targetPath = args[2]

    val model = parse("$rootPath/$sourcePath")

    val markdown = generateGitHubReleaseMarkDown(model)

    val url = URLEncoder.encode(markdown, "UTF-8")

    val target = File("$rootPath/$targetPath")
    target.outputStream().use { output ->
        output.write(url.toByteArray())
        output.flush()
    }
}