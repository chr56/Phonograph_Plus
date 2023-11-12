/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.output.html.generateHTML
import util.phonograph.releasenote.ReleaseNote

fun generateHtml(model: ReleaseNote) {
    val html = generateHTML(model)
    for ((lang, text) in html) {
        println("lang $lang:")
        println(text)
    }
}