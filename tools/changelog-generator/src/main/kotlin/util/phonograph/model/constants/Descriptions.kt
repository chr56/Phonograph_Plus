/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model.constants

const val OVERFLOWED_MESSAGE = "...(Visit project homepage to see full changelogs)"

const val SIGNING_KEY_WARNING_EN = "We have changed the signing key since this version. You need uninstall first before upgrading. \nHowever, if your device runs Android 9 and higher, you can install `IntermediaRelease` variants to transit smoothly without uninstalling first."
const val SIGNING_KEY_WARNING_ZH = "自该版本起，我们使用了新的签名密钥；更新前需要先卸载再安装；\n但是，若设备 Android 版本大于9，可安装带有 `IntermediaRelease` 的安装包，则可无需卸载即可迁到新签名密钥！"

const val PREVIEW_WARNING_EN =
    "This is a _Preview Channel_ Release (identified by `preview` suffix in the package name), stability and quality are not guaranteed."
const val PREVIEW_WARNING_ZH = "此为预览通道版本 (包名后缀`preview`), 不保证可靠性!"

const val EAP_WARNING_EN =
    "This is an _Early Access Preview Channel_ Release (with package name suffix `eap`), as an experimental version for the next major release. New features are tentative and unstable; use at your own risk!"
const val EAP_WARNING_ZH = "此为提前预览通道版本（包名后缀`eap`），为下一大版本的不稳定试验性版本！"

const val COMMIT_LOG_PREFIX = "**Commit log**: "

const val VARIANTS_DESCRIPTION_TITLE = "Version Variants Description / 版本说明"
const val VARIANTS_DESCRIPTION_BODY = """
-> [Version Guide](docs/Version_Guide.md) / [版本指南](docs/Version_Guide_ZH.md)

**TL;DR**: If you are a user of Android 10, 7 and 7.1, please consider to use `Legacy`; If not, use `Modern`. The `Fdroid` is identical to the version on F‑droid; and the signature is same with others. Recently, you may need Intermediate variant to transit to new signing key.
**太长不看**: 若为 Android 10 或 7 及 7.1 用户，请考虑使用 `Legacy` 版本；否则，请使用 `Modern` 版本。另外带 `Fdroid` 为 F-droid 上完全相同的版本，签名与其他版本相同。近期可能需要 Intermediate 版本过渡至新签名。
"""


const val DOWNLOAD_LINK_TEMPLATE = """
__Download Links__: [Modern Variant](%s) | [Legacy Variant](%s) | [Intermediate Variant for transition](%s) 
"""


const val SIGNING_KEY_NOTICE_EN = "Since version 2.0.0-dev3, signing key of Phonograph Plus has been replaced and rotated, as one of major breaks!\nYou need uninstall first before upgrading. However, if your device runs Android 9 and higher, you can install `IntermediaRelease` variants to transit smoothly without uninstalling first (then you can install any other variants)."
const val SIGNING_KEY_NOTICE_ZH = "自 v2.0.0-dev3 起，签名密钥已被更替或轮转（破坏性变更项之一）！\n更新前需要先卸载再安装；但是，若设备 Android 版本大于9，可安装带有 `IntermediaRelease` 的安装包，则可无需卸载即可迁到新签名密钥（随后可安装其他版本）！"