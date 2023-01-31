/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import java.io.File

fun main() {
    val model = parse(File("tools/changelog-generator/FormatExample.md"))
    println(model)
    println("===========================================")

    val md = generateGitHubReleaseMarkDown(model)
    println(md)
    println("===========================================")

    val html = generateHTML(model)
    for ((lang, text) in html) {
        println("lang $lang:")
        println(text)
    }
    println("===========================================")
}