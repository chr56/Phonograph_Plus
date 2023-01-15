/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.tasks.TaskContainer
import tools.release.text.canonicalName

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