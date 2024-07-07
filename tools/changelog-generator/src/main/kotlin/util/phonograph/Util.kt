/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}

val yamlParser = Yaml(
    configuration = YamlConfiguration(
        breakScalarsAt = 148,
    )
)

private val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH)
fun dateString(stamp: Long): String {
    return dateFormatter.format(Date(stamp * 1000))
}

fun checkFile(file: File, override: Boolean) {
    if (file.exists()) {
        require(file.isFile) { "${file.path} is not a file!" }
        if (override) file.delete()
    } else {
        file.createNewFile()
    }
}

fun writeToFile(string: String, path: String, override: Boolean = true) {
    val file = File(path)
    writeToFile(string, file, override)
}

fun writeToFile(data: String, file: File, override: Boolean = true) {
    checkFile(file, override)
    file.bufferedWriter().use {
        it.write(data)
    }
}

private fun File.bufferedWriter(): BufferedWriter =
    outputStream().writer(Charsets.UTF_8).buffered(4096)

/**
 * Escapes Markdown characters
 */
fun escapeMarkdown(origin: String): String {
    return Regex("""([_*\[\]()~`>\#\+\-=|\.!\{\}])""").replace(origin) { "\\${it.value}" }
}