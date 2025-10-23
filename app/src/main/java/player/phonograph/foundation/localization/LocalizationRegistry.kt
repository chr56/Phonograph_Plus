/*
 *  Copyright (c) 2022~2025 chr_56
 */

@file:JvmName("LocalizationRegistry")

package player.phonograph.foundation.localization

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import java.util.Locale

fun Locale.display(currentLocale: Locale): String {
    val nameInCurrentLocale = this.getDisplayName(currentLocale)
    val nameInNaiveSpeaker = this.getDisplayName(this)

    val tag = this.toLanguageTag()
    val nameInEnglish = this.getDisplayName(Locale.ENGLISH)
    val note = if (currentLocale.language != Locale.ENGLISH.language) "$tag, $nameInEnglish" else tag

    return "$nameInCurrentLocale - $nameInNaiveSpeaker ($note)"
}

val availableLanguageTag = arrayOf(
    "en-rUS",
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
        localeOf(lang, region)
    } else {
        // without regions
        localeOf(tag)
    }
}

fun localeOf(lang: String, region: String): Locale =
    if (SDK_INT >= VERSION_CODES.BAKLAVA) {
        Locale.of(lang, region)
    } else {
        @Suppress("DEPRECATION")
        Locale(lang, region)
    }

fun localeOf(lang: String): Locale =
    if (SDK_INT >= VERSION_CODES.BAKLAVA) {
        Locale.of(lang)
    } else {
        @Suppress("DEPRECATION")
        Locale(lang)
    }
