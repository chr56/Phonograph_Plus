/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import kotlinx.serialization.json.Json

val parser = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}