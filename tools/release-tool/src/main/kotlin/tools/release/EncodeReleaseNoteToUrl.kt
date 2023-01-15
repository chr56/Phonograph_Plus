/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLEncoder

open class EncodeReleaseNoteToUrlTask : DefaultTask() {
    @TaskAction
    fun encodeReleaseNoteToUrl() {
        encodeReleaseNoteToUrl(
            inputReleaseNote,
            outputReleaseNote
        )
    }

    companion object {
        val inputReleaseNote = File("ReleaseNote.md")
        val outputReleaseNote = File("ReleaseNote.url.txt")
    }
}

fun encodeReleaseNoteToUrl(inputFile: File, outputFile: File): Boolean =
    if (inputFile.exists()) {
        val raw = inputFile.readText()
        val result = URLEncoder.encode(raw, "UTF-8")
        outputFile.outputStream().use { output ->
            output.write(result.toByteArray())
            output.flush()
        }
        true
    } else {
        println("ReleaseNote ${inputFile.name} doesn't exist!")
        false
    }