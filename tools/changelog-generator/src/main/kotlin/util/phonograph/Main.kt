/*
 *  Copyright (c) 2023 chr_56
 */

package util.phonograph

import util.phonograph.html.updateChangelogs
import util.phonograph.model.ReleaseMetadata
import util.phonograph.utils.yamlParser
import kotlinx.serialization.decodeFromString
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("No Command!")
    } else {
        val command = args.first()
        val parameters = args.drop(1)
        process(command, parameters)
    }
}

fun process(command: String, parameters: List<String>) {
    if (parameters.size >= 2) {
        val rootPath = parameters[0]
        val sourcePath = parameters[1]
        println("Parse Release Metadata ...")
        val model = parseReleaseMetadataYaml(File("$rootPath/$sourcePath"))
        println("Process ...")
        when (command) {
            CMD_GENERATE_GITHUB_RELEASE_NOTE -> generateGithubReleaseNote(model, "$rootPath/${parameters[2]}")
            CMD_GENERATE_ESCAPED_MARKDOWN    -> generateEscapedMarkdownReleaseNote(model, "$rootPath/${parameters[2]}")
            CMD_GENERATE_VERSION_JSON        -> generateVersionJson(model, "$rootPath/${parameters[2]}")
            CMD_GENERATE_HTML                -> generateHtml(model)
            CMD_GENERATE_FDROID_METADATA     -> generateFdroidMetadata(model, rootPath)
            CMD_GENERATE_ENCODED_URL         -> generateEncodedUrl(model, "$rootPath/${parameters[2]}")
            CMD_REFRESH_CHANGELOGS           -> updateChangelogs(model, File("$rootPath/${parameters[2]}"))
            else                             -> println("Unknown command")
        }
    } else {
        throw IllegalArgumentException("No Release Note file parameters!")
    }
}

fun parseReleaseMetadataYaml(file: File): ReleaseMetadata {
    val text = file.readText()
    val metadata = yamlParser.decodeFromString<ReleaseMetadata>(text)
    println(metadata)
    return metadata
}

const val CMD_GENERATE_GITHUB_RELEASE_NOTE = "GenerateGithubReleaseNote"
const val CMD_GENERATE_ESCAPED_MARKDOWN = "GenerateEscapedMarkdownReleaseNote"
const val CMD_GENERATE_VERSION_JSON = "GenerateVersionJson"
const val CMD_GENERATE_HTML = "GenerateHTML"
const val CMD_GENERATE_FDROID_METADATA = "GenerateFdroidMetadata"
const val CMD_GENERATE_ENCODED_URL = "GenerateEncodedUrl"
const val CMD_REFRESH_CHANGELOGS = "RefreshChangelogs"