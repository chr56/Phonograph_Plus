/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.bugreport.model

import player.phonograph.ui.activities.bugreport.model.github.ExtraInfo

class Report(
    val title: String,
    val description: String,
    private val deviceInfo: DeviceInfo,
    private val extraInfo: ExtraInfo
) {
    val body: String
        get() {
            return """
            $description
            
            -
            
            ${deviceInfo.toMarkdown()}
            -
            ${extraInfo.toMarkdown()}
            """.trimIndent()
        }
}
