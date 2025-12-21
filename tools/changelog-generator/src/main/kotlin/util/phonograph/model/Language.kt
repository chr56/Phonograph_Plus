/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model

import kotlinx.serialization.Serializable

@Serializable
enum class Language(val displayName: String, val fullCode: String) {
    EN("English", "en-US"),
    ZH("Chinese", "zh-CN"),
    ;
}