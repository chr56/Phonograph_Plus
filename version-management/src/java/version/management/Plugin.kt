/*
 * Copyright (c) 2022 chr_56
 */

package version.management

import org.gradle.api.Plugin
import org.gradle.api.Project
import version.management.Util.encodeReleaseNoteToUrl

class Plugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("encodeReleaseNoteToUrl", EncodeReleaseNoteToUrlTask::class.java)
    }
}
