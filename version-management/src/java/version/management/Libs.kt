/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("SpellCheckingInspection")

package version.management

object Deps {
    object AndroidX {
        const val core = "androidx.core:core-ktx:1.9.0"
        const val appcompat = "androidx.appcompat:appcompat:1.5.1"
        const val activity = "androidx.activity:activity-ktx:1.6.0"
        const val fragment = "androidx.fragment:fragment-ktx:1.5.3"
        const val lifecycle_runtime = "androidx.lifecycle:lifecycle-runtime-ktx:2.5.1"

        const val annotation = "androidx.annotation:annotation:1.4.0"
        const val preference = "androidx.preference:preference-ktx:1.2.0"

        const val recyclerview = "androidx.recyclerview:recyclerview:1.2.1"

        const val constraintlayout = "androidx.constraintlayout:constraintlayout:2.1.4"
        const val percentlayout = "androidx.percentlayout:percentlayout:1.0.0"
        const val swiperefreshlayout = "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"

        const val media = "androidx.media:media:1.6.0"
        const val cardview = "androidx.cardview:cardview:1.0.0"
        const val palette = "androidx.palette:palette-ktx:1.0.0"
    }

    object Compose {
        const val comilerVersion = "1.3.1"
        const val libVersion = "1.2.1"

        const val ui = "androidx.compose.ui:ui:$libVersion"
        const val ui_tooling = "androidx.compose.ui:ui-tooling:$libVersion"
        const val foundation = "androidx.compose.foundation:foundation:$libVersion"
        const val material = "androidx.compose.material:material:$libVersion"

        const val activity = "androidx.activity:activity-compose:1.4.0"
    }

    const val google_material = "com.google.android.material:material:1.6.1"

    const val okhttp3 = "com.squareup.okhttp3:okhttp:4.10.0"
    const val retrofit2 = "com.squareup.retrofit2:retrofit:2.9.0"

    const val coil = "io.coil-kt:coil:2.2.0"

    const val material_dialogs_version = "3.3.0"
    const val material_dialogs_core = "com.afollestad.material-dialogs:core:$material_dialogs_version"
    const val material_dialogs_input = "com.afollestad.material-dialogs:input:$material_dialogs_version"
    const val material_dialogs_color = "com.afollestad.material-dialogs:color:$material_dialogs_version"


    private const val mdVersion = "0.0.9"
    const val mdColorRes = "com.github.chr56.material-tools:mdColorRes:$mdVersion"
    const val mdUtil = "com.github.chr56.material-tools:mdUtil:$mdVersion"
    const val mdPref = "com.github.chr56.material-tools:mdPref:$mdVersion"
    const val mdTint = "com.github.chr56.material-tools:mdTint:$mdVersion"

    const val android_menu_dsl = "io.github.chr56:android-menu-dsl:0.1.0"

    const val SeekArc = "com.github.chr56:SeekArc:1.0"
    const val AndroidSlidingUpPanel_kabouzeid = "com.github.kabouzeid:AndroidSlidingUpPanel:6"

    const val recyclerview_fastscroll = "com.simplecityapps:recyclerview-fastscroll:2.0.1"

    const val kotlinx_serialization_json = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0"

    const val licensesdialog = "de.psdev.licensesdialog:licensesdialog:2.1.0"
    const val jaudiotagger = "com.github.AdrienPoupa:jaudiotagger:2.2.3"
    const val observablescrollview = "com.github.ksoichiro:android-observablescrollview:1.6.0"
    const val material_intro = "com.heinrichreimersoftware:material-intro:2.0.0"
    const val advrecyclerview = "com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0"
}
