/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

const val demoFile = "tools/changelog-generator/FormatExample.md"

fun main(args: Array<String>) {
    val model = parse(demoFile)
    println("===========================================")
    println("Models: $model")
    println("===========================================")

    val md = generateGitHubReleaseMarkDown(model)
    println(md)
    exportGitHubReleaseMarkDown(md)
    println("===========================================")

    val html = generateHTML(model)
    for ((lang, text) in html) {
        println("lang $lang:")
        println(text)
    }
    println("===========================================")
}