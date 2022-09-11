/*
 * Copyright (c) 2022 chr_56
 */

package util.phonograph.deviceinfo

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
