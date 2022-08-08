/*
 * Copyright (c) 2022 chr_56
 */

package version.management

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import version.management.Util.encodeReleaseNoteToUrl

open class EncodeReleaseNoteToUrlTask : DefaultTask() {
    @TaskAction
    fun encodeReleaseNoteToUrl() {
        encodeReleaseNoteToUrl(
            inputReleaseNote,
            outputReleaseNote
        )
    }
}
