/*
 *  Copyright (c) 2022~2024 chr_56
 */

package lib.phonograph.theme.internal

import android.content.Context
import android.os.Build.VERSION_CODES.S
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi

object MonetColor {

    @RequiresApi(S)
    @ColorInt
    fun accent1Color(context: Context, @Depth deep: Int): Int =
        dynasticColor(context, ACCENT1, deep)

    @RequiresApi(S)
    @ColorInt
    fun accent2Color(context: Context, @Depth deep: Int): Int =
        dynasticColor(context, ACCENT2, deep)

    @RequiresApi(S)
    @ColorInt
    fun accent3Color(context: Context, @Depth deep: Int): Int =
        dynasticColor(context, ACCENT3, deep)

    @RequiresApi(S)
    @ColorInt
    fun neutral1Color(context: Context, @Depth deep: Int): Int =
        dynasticColor(context, NEUTRAL1, deep)

    @RequiresApi(S)
    @ColorInt
    fun neutral2Color(context: Context, @Depth deep: Int): Int =
        dynasticColor(context, NEUTRAL2, deep)

    @RequiresApi(S)
    @ColorInt
    private fun dynasticColor(context: Context, @Type type: Int, @Depth deep: Int): Int {
        return when (type) {
            ACCENT1 -> when (deep) {
                DEPTH_0 -> context.getColor(android.R.color.system_accent1_0)
                DEPTH_10 -> context.getColor(android.R.color.system_accent1_10)
                DEPTH_50 -> context.getColor(android.R.color.system_accent1_50)
                DEPTH_100 -> context.getColor(android.R.color.system_accent1_100)
                DEPTH_200 -> context.getColor(android.R.color.system_accent1_200)
                DEPTH_300 -> context.getColor(android.R.color.system_accent1_300)
                DEPTH_400 -> context.getColor(android.R.color.system_accent1_400)
                DEPTH_500 -> context.getColor(android.R.color.system_accent1_500)
                DEPTH_600 -> context.getColor(android.R.color.system_accent1_600)
                DEPTH_700 -> context.getColor(android.R.color.system_accent1_700)
                DEPTH_800 -> context.getColor(android.R.color.system_accent1_800)
                DEPTH_900 -> context.getColor(android.R.color.system_accent1_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_accent1_1000)
                else -> -1
            }

            ACCENT2 -> when (deep) {
                DEPTH_0 -> context.getColor(android.R.color.system_accent2_0)
                DEPTH_10 -> context.getColor(android.R.color.system_accent2_10)
                DEPTH_50 -> context.getColor(android.R.color.system_accent2_50)
                DEPTH_100 -> context.getColor(android.R.color.system_accent2_100)
                DEPTH_200 -> context.getColor(android.R.color.system_accent2_200)
                DEPTH_300 -> context.getColor(android.R.color.system_accent2_300)
                DEPTH_400 -> context.getColor(android.R.color.system_accent2_400)
                DEPTH_500 -> context.getColor(android.R.color.system_accent2_500)
                DEPTH_600 -> context.getColor(android.R.color.system_accent2_600)
                DEPTH_700 -> context.getColor(android.R.color.system_accent2_700)
                DEPTH_800 -> context.getColor(android.R.color.system_accent2_800)
                DEPTH_900 -> context.getColor(android.R.color.system_accent2_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_accent2_1000)
                else -> -1
            }

            ACCENT3 -> when (deep) {
                DEPTH_0 -> context.getColor(android.R.color.system_accent3_0)
                DEPTH_10 -> context.getColor(android.R.color.system_accent3_10)
                DEPTH_50 -> context.getColor(android.R.color.system_accent3_50)
                DEPTH_100 -> context.getColor(android.R.color.system_accent3_100)
                DEPTH_200 -> context.getColor(android.R.color.system_accent3_200)
                DEPTH_300 -> context.getColor(android.R.color.system_accent3_300)
                DEPTH_400 -> context.getColor(android.R.color.system_accent3_400)
                DEPTH_500 -> context.getColor(android.R.color.system_accent3_500)
                DEPTH_600 -> context.getColor(android.R.color.system_accent3_600)
                DEPTH_700 -> context.getColor(android.R.color.system_accent3_700)
                DEPTH_800 -> context.getColor(android.R.color.system_accent3_800)
                DEPTH_900 -> context.getColor(android.R.color.system_accent3_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_accent3_1000)
                else -> -1
            }

            NEUTRAL1 -> when (deep) {
                DEPTH_0 -> context.getColor(android.R.color.system_neutral1_0)
                DEPTH_10 -> context.getColor(android.R.color.system_neutral1_10)
                DEPTH_50 -> context.getColor(android.R.color.system_neutral1_50)
                DEPTH_100 -> context.getColor(android.R.color.system_neutral1_100)
                DEPTH_200 -> context.getColor(android.R.color.system_neutral1_200)
                DEPTH_300 -> context.getColor(android.R.color.system_neutral1_300)
                DEPTH_400 -> context.getColor(android.R.color.system_neutral1_400)
                DEPTH_500 -> context.getColor(android.R.color.system_neutral1_500)
                DEPTH_600 -> context.getColor(android.R.color.system_neutral1_600)
                DEPTH_700 -> context.getColor(android.R.color.system_neutral1_700)
                DEPTH_800 -> context.getColor(android.R.color.system_neutral1_800)
                DEPTH_900 -> context.getColor(android.R.color.system_neutral1_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_neutral1_1000)
                else -> -1
            }

            NEUTRAL2 -> when (deep) {
                DEPTH_0 -> context.getColor(android.R.color.system_neutral2_0)
                DEPTH_10 -> context.getColor(android.R.color.system_neutral2_10)
                DEPTH_50 -> context.getColor(android.R.color.system_neutral2_50)
                DEPTH_100 -> context.getColor(android.R.color.system_neutral2_100)
                DEPTH_200 -> context.getColor(android.R.color.system_neutral2_200)
                DEPTH_300 -> context.getColor(android.R.color.system_neutral2_300)
                DEPTH_400 -> context.getColor(android.R.color.system_neutral2_400)
                DEPTH_500 -> context.getColor(android.R.color.system_neutral2_500)
                DEPTH_600 -> context.getColor(android.R.color.system_neutral2_600)
                DEPTH_700 -> context.getColor(android.R.color.system_neutral2_700)
                DEPTH_800 -> context.getColor(android.R.color.system_neutral2_800)
                DEPTH_900 -> context.getColor(android.R.color.system_neutral2_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_neutral2_1000)
                else -> -1
            }

            else -> -1
        }
    }

    const val ACCENT1 = 2
    const val ACCENT2 = 4
    const val ACCENT3 = 8
    const val NEUTRAL1 = 16
    const val NEUTRAL2 = 32


    @IntDef(ACCENT1, ACCENT2, ACCENT3, NEUTRAL1, NEUTRAL2)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type


    @IntDef(
        DEPTH_0,
        DEPTH_10,
        DEPTH_50,
        DEPTH_100,
        DEPTH_200,
        DEPTH_300,
        DEPTH_400,
        DEPTH_500,
        DEPTH_600,
        DEPTH_700,
        DEPTH_800,
        DEPTH_900,
        DEPTH_1000,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Depth

    const val DEPTH_0 = 0
    const val DEPTH_10 = 10
    const val DEPTH_50 = 50
    const val DEPTH_100 = 100
    const val DEPTH_200 = 200
    const val DEPTH_300 = 300
    const val DEPTH_400 = 400
    const val DEPTH_500 = 500
    const val DEPTH_600 = 600
    const val DEPTH_700 = 700
    const val DEPTH_800 = 800
    const val DEPTH_900 = 900
    const val DEPTH_1000 = 1000

    @JvmInline
    value class MonetColorPalette internal constructor(internal val value: Int) {
        constructor(@Type type: Int, @Depth depth: Int) : this((type shl SHIFT) + depth)

        val type: Int @Type get() = value ushr SHIFT
        val depth: Int @Depth get() = value shl SHIFT ushr SHIFT

        @RequiresApi(S)
        @ColorInt
        fun color(context: Context): Int = dynasticColor(context, type, depth)

        companion object {
            private const val SHIFT = 16
        }
    }

    val defaultMonetPrimaryColor get() = MonetColorPalette(ACCENT1, DEPTH_400)
    val defaultMonetAccentColor get() = MonetColorPalette(ACCENT1, DEPTH_700)

}