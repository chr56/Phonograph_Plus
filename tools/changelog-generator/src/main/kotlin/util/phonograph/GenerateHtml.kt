/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph

import util.phonograph.html.generateHTML
import util.phonograph.model.ReleaseMetadata

fun generateHtml(model: ReleaseMetadata) {
    val html = generateHTML(model)
    for ((lang, text) in html) {
        println("lang $lang:")
        println(text)
    }
}