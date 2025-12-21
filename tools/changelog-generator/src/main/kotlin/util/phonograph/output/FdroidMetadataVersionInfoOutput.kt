/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.model.OutputFormat
import util.phonograph.model.ReleaseMetadata
import java.io.Writer


class FdroidMetadataVersionInfoOutput(val model: ReleaseMetadata) : OutputFormat {

    companion object {
        private const val KEY_LATEST_VERSION_NAME = "latest_version_name"
        private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    }

    private fun generateFdroidMetadataVersionInfo(model: ReleaseMetadata): String =
        buildString {
            appendLine("$KEY_LATEST_VERSION_NAME=${model.version}")
            appendLine("$KEY_LATEST_VERSION_CODE=${model.versionCode}")
        }

    override fun write(target: Writer) {
        if (!model.channel.isPreview) {
            target.write(generateFdroidMetadataVersionInfo(model))
        } else {
            println(" (${model.channel} channel, ignored!)")
        }
    }
}

