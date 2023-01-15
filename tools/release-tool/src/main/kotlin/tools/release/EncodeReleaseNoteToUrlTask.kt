/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import tools.release.text.encodeReleaseNoteToUrl
import java.io.File

open class EncodeReleaseNoteToUrlTask : DefaultTask() {
    @TaskAction
    fun encode() {
        encodeReleaseNoteToUrl(inputReleaseNote, outputReleaseNote)
    }

    companion object {
        val inputReleaseNote = File("ReleaseNote.md")
        val outputReleaseNote = File("ReleaseNote.url.txt")
    }
}