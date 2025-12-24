/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model.constants

const val OVERFLOWED_MESSAGE = "...(Visit project homepage to see full changelogs)"


const val PREVIEW_WARNING_EN = "This is a _Preview Channel_ Release (identified by `preview` suffix in the package name), stability and quality are not guaranteed."
const val PREVIEW_WARNING_ZH = "此为预览通道版本 (包名后缀`preview`), 不保证可靠性!"

const val EAP_WARNING_EN = "This is a _Early Access Preview Channel_ Release (with package name suffix `eap`), as an experimental version for the next major release. New features are tentative and unstable; use at your own risk!"
const val EAP_WARNING_ZH = "此为提前预览通道版本（包名后缀`eap`），为下一大版本的不稳定试验性版本！"

const val COMMIT_LOG_PREFIX = "**Commit log**: "

const val VARIANTS_DESCRIPTION_TITLE = "Version Variants Description / 版本说明"
const val VARIANTS_DESCRIPTION_BODY = """
-> [Version Guide](docs/Version_Guide.md) / [版本指南](docs/Version_Guide_ZH.md)

**TL;DR**: If you are a user of Android 7 ~ 10, please consider to use `Legacy`; If not, use `Modern`. The `Fdroid` is identical to the version on F‑droid; and the signature is same with others.
**太长不看**: 若为 Android 7 ~ 10 用户，请考虑使用 `Legacy` 版本；否则，请使用 `Modern` 版本。另外带 `Fdroid` 为 F-droid 上完全相同的版本，签名与其他版本相同。
"""


const val DOWNLOAD_LINK_TEMPLATE = """
__Download Links__: [Modern Variant](%s) | [Legacy Variant](%s) 
"""