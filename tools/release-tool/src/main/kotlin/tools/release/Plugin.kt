/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer

class Plugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.registerEncodeReleaseNoteToUrlTask()
    }
}

internal fun Project.registerEncodeReleaseNoteToUrlTask() {
    tasks.register("encodeReleaseNoteToUrl", EncodeReleaseNoteToUrlTask::class.java)
}

fun TaskContainer.registerPublishTask(
    appName: String,
    appVersionName: String,
    variant: ApplicationVariant
) {
    val name: String = variant.canonicalName
    register(
        "publish$name", PublishArtifactsTask::class.java,
        appName, appVersionName, variant
    ).configure {
        it.dependsOn("assemble$name")
    }
}

