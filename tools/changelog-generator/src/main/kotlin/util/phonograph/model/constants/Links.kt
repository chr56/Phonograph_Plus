/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model.constants

const val DOWNLOAD_LINK_GITHUB_HOME_LABEL = "Github Release (Website)"
const val DOWNLOAD_LINK_GITHUB_MODERN_LABEL = "Github Release Modern Variant APK (File Download Link)"
const val DOWNLOAD_LINK_GITHUB_LEGACY_LABEL = "Github Release Legacy Variant APK (File Download Link)"

const val GITHUB_LINK_PREFIX = "https://github.com/chr56/Phonograph_Plus"
const val GITHUB_RELEASE_LINK = "$GITHUB_LINK_PREFIX/releases/tag/%s"
const val GITHUB_DOWNLOAD_LINK = "$GITHUB_LINK_PREFIX/releases/download/%s/PhonographPlus_%s_%s.apk"
const val GITHUB_COMPARE_LINK = "$GITHUB_LINK_PREFIX/compare/%s...%s"

fun releaseLink(version: String) =
    String.format(GITHUB_RELEASE_LINK, version)

fun downloadLink(tag: String, version: String, variant: String) =
    String.format(GITHUB_DOWNLOAD_LINK, tag, version, variant)

fun compareLink(fromTag: String, toTag: String): String =
    String.format(GITHUB_COMPARE_LINK, fromTag, toTag)