/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.bugreport.model

import player.phonograph.ui.activities.bugreport.model.github.ExtraInfo

class Report(
    val title: String,
    description: String,
    private val deviceInfo: DeviceInfo,
    private val extraInfo: ExtraInfo
) {
    val description: String = description
        get() {
            return """
            $field
            
            -
            
            ${deviceInfo.toMarkdown()}
            
            ${extraInfo.toMarkdown()}
            """.trimIndent()
        }
}
