/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model

import kotlinx.serialization.Serializable


@Serializable
enum class TargetVariant(val favorName: String) {
    MODERN("Modern"),
    LEGACY("Legacy"),
    ;
}

@Serializable
enum class ReleaseChannel(val favorName: String, val tagPrefix: String, val isPreview: Boolean = false) {
    EAP("Next", "eap_", isPreview = true),
    PREVIEW("Preview", "preview_", isPreview = true),
    STABLE("Stable", "v"),
    LTS("Stable", "v"),
    ;

    fun gitTagNameOf(version: String): String = "${tagPrefix}$version"
}

@Serializable
enum class BuildType(val string: String) {
    RELEASE("Release"),
    DEBUG("Debug"),
    ;
}

fun variantQualifierOf(variant: TargetVariant, channel: ReleaseChannel, buildType: BuildType): String =
    "${variant.favorName}${channel.favorName}${buildType.string}"