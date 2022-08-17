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
    val note = if (this != Locale.ENGLISH) "$tag, $nameInEnglish" else tag

    return "$nameInCurrentLocale - $nameInNaiveSpeaker ($note)"
}

@Suppress("SpellCheckingInspection")
val availableLanguageTag = arrayOf(
    "en",
    "en-CA",
    "en-GB",
    "ar",
    "bg",
    "cs",
    "de",
    "el",
    "es-ES",
    "es-US",
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
    "pt-BR",
    "pt-PT",
    "ro",
    "ru",
    "tr",
    "uk",
    "vi",
    "zh-CN",
    "zh-TW"
)

fun getAvailableLanguage(): Array<Locale> {
    val tags = availableLanguageTag
    val arrayOfLocales = Array(tags.size) {
        Locale(tags[it])
    }
    return arrayOfLocales
}

fun getAvailableLanguageNames(currentLocale: Locale): Array<String> {
    val tags = availableLanguageTag
    val list = Array(tags.size) {
        Locale(tags[it]).display(currentLocale)
    }
    return list
}
