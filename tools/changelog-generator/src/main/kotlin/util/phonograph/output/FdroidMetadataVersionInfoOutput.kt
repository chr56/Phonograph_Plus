/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.releasenote.ReleaseChannel
import util.phonograph.releasenote.ReleaseNote
import java.io.Writer



private const val LATEST_VERSION_NAME = "latest_version_name"
private const val LATEST_VERSION_CODE = "latest_version_code"

class FdroidMetadataVersionInfoOutput(val model: ReleaseNote) : OutputFormat {

    private fun generateFdroidMetadataVersionInfo(model: ReleaseNote): String =
        buildString {
            appendLine("$LATEST_VERSION_NAME=${model.version}")
            appendLine("$LATEST_VERSION_CODE=${model.versionCode}")
        }

    override fun write(target: Writer) {
        if (model.channel == ReleaseChannel.STABLE) {
            val text = generateFdroidMetadataVersionInfo(model)
            target.write(text)
        } else {
            println(" (${model.channel} channel, ignored!)")
        }
    }
}

