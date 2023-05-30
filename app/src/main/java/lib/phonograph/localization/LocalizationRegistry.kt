/*
 * Copyright (c) 2022 chr_56
 */

@file:JvmName("LocalizationRegistry")

package lib.phonograph.localization

import java.util.*

fun Locale.display(currentLocale: Locale): String {
    val nameInCurrentLocale = this.getDisplayName(currentLocale)
    val nameInNaiveSpeaker = this.getDisplayName(this)

    val tag = this.toLanguageTag()
    val nameInEnglish = this.getDisplayName(Locale.ENGLISH)
    val note = if (currentLocale.language != Locale.ENGLISH.language) "$tag, $nameInEnglish" else tag

    return "$nameInCurrentLocale - $nameInNaiveSpeaker ($note)"
}

@Suppress("SpellCheckingInspection")
val availableLanguageTag = arrayOf(
    "en",
    "en-rCA",
    "en-rGB",
    "ar",
    "bg",
    "cs",
    "de",
    "el",
    "es-rES",
    "es-rUS",
    "fi",
    "fr",
    "hr",
    "hu",
    "in", //
    "it",
    "iw", //
    "ja",
    "ko",
    "nl",
    "pl",
    "pt-rBR",
    "pt-rPT",
    "ro",
    "ru",
    "tr",
    "uk",
    "vi",
    "zh-rCN",
    "zh-rTW"
)

fun getAvailableLanguage(): Array<Locale> {
    val tags = availableLanguageTag
    val arrayOfLocales = Array(tags.size) {
        parseAndroidTag(tags[it])
    }
    return arrayOfLocales
}

fun getAvailableLanguageNames(currentLocale: Locale): Array<String> {
    val tags = availableLanguageTag
    val list = Array(tags.size) {
        parseAndroidTag(tags[it]).display(currentLocale)
    }
    return list
}

fun parseAndroidTag(tag: String): Locale {
    return if (tag.matches(Regex(".*-r.*"))) {
        // with region
        val r = tag.split("-r", limit = 2)
        val lang = r[0]
        val region = r[1]
        Locale(lang, region)
    } else {
        // without regions
        Locale(tag)
    }
}
