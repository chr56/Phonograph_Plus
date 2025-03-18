/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.version

enum class ReleaseChannel(val determiner: String) {
    Preview("preview"),
    Stable("stable"),
    LTS("lts"),
    ;
}