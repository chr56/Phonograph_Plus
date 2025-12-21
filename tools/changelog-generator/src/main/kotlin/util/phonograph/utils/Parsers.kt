/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.utils

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json

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