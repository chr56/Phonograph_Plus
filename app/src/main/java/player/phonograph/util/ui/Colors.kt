/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.ui

import util.theme.materials.MaterialColor
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import android.content.Context
import android.os.Build.VERSION_CODES.S


object ColorPalette {

    enum class Variant {
        Primary, Accent;
    }

    //region Dynamic Colors
    @RequiresApi(S)
    fun dynamicColors(context: Context) = intArrayOf(
        context.getColor(android.R.color.system_accent1_400),
        context.getColor(android.R.color.system_accent2_400),
        context.getColor(android.R.color.system_accent3_400),
        context.getColor(android.R.color.system_neutral1_500),
        context.getColor(android.R.color.system_neutral2_500),
    )

    @RequiresApi(S)
    fun allDynamicColors(context: Context) = arrayOf(
        intArrayOf(
            context.getColor(android.R.color.system_accent1_100),
            context.getColor(android.R.color.system_accent1_200),
            context.getColor(android.R.color.system_accent1_300),
            context.getColor(android.R.color.system_accent1_400),
            context.getColor(android.R.color.system_accent1_500),
            context.getColor(android.R.color.system_accent1_600),
            context.getColor(android.R.color.system_accent1_700),
            context.getColor(android.R.color.system_accent1_800),
            context.getColor(android.R.color.system_accent1_900),
        ),
        intArrayOf(
            context.getColor(android.R.color.system_accent2_100),
            context.getColor(android.R.color.system_accent2_200),
            context.getColor(android.R.color.system_accent2_300),
            context.getColor(android.R.color.system_accent2_400),
            context.getColor(android.R.color.system_accent2_500),
            context.getColor(android.R.color.system_accent2_600),
            context.getColor(android.R.color.system_accent2_700),
            context.getColor(android.R.color.system_accent2_800),
            context.getColor(android.R.color.system_accent2_900),
        ),
        intArrayOf(
            context.getColor(android.R.color.system_accent3_100),
            context.getColor(android.R.color.system_accent3_200),
            context.getColor(android.R.color.system_accent3_300),
            context.getColor(android.R.color.system_accent3_400),
            context.getColor(android.R.color.system_accent3_500),
            context.getColor(android.R.color.system_accent3_600),
            context.getColor(android.R.color.system_accent3_700),
            context.getColor(android.R.color.system_accent3_800),
            context.getColor(android.R.color.system_accent3_900),
        ),
        intArrayOf(
            // context.getColor(android.R.color.system_neutral1_0),
            context.getColor(android.R.color.system_neutral1_100),
            context.getColor(android.R.color.system_neutral1_200),
            context.getColor(android.R.color.system_neutral1_300),
            context.getColor(android.R.color.system_neutral1_400),
            context.getColor(android.R.color.system_neutral1_500),
            context.getColor(android.R.color.system_neutral1_600),
            context.getColor(android.R.color.system_neutral1_700),
            context.getColor(android.R.color.system_neutral1_800),
            context.getColor(android.R.color.system_neutral1_900),
            // context.getColor(android.R.color.system_neutral1_1000),
        ),
        intArrayOf(
            // context.getColor(android.R.color.system_neutral2_0),
            context.getColor(android.R.color.system_neutral2_100),
            context.getColor(android.R.color.system_neutral2_200),
            context.getColor(android.R.color.system_neutral2_300),
            context.getColor(android.R.color.system_neutral2_400),
            context.getColor(android.R.color.system_neutral2_500),
            context.getColor(android.R.color.system_neutral2_600),
            context.getColor(android.R.color.system_neutral2_700),
            context.getColor(android.R.color.system_neutral2_800),
            context.getColor(android.R.color.system_neutral2_900),
            // context.getColor(android.R.color.system_neutral2_1000),
        ),
    )
    //endregion

    val colors: IntArray
        get() = intArrayOf(
            MaterialColor.Red._A400.asColor,
            MaterialColor.Pink._A400.asColor,
            MaterialColor.Purple._A400.asColor,
            MaterialColor.DeepPurple._A400.asColor,
            MaterialColor.Indigo._A400.asColor,
            MaterialColor.Blue._A400.asColor,
            MaterialColor.LightBlue._A400.asColor,
            MaterialColor.Cyan._A400.asColor,
            MaterialColor.Teal._A400.asColor,
            MaterialColor.Green._A400.asColor,
            MaterialColor.LightGreen._A400.asColor,
            MaterialColor.Lime._A400.asColor,
            MaterialColor.Yellow._A400.asColor,
            MaterialColor.Amber._A400.asColor,
            MaterialColor.Orange._A400.asColor,
            MaterialColor.DeepOrange._A400.asColor,
            MaterialColor.Brown._600.asColor,
            MaterialColor.BlueGrey._600.asColor,
            MaterialColor.Grey._600.asColor
        )
    val subColors
        get() = arrayOf(
            intArrayOf(
                MaterialColor.Red._50.asColor,
                MaterialColor.Red._100.asColor,
                MaterialColor.Red._200.asColor,
                MaterialColor.Red._300.asColor,
                MaterialColor.Red._400.asColor,
                MaterialColor.Red._500.asColor,
                MaterialColor.Red._600.asColor,
                MaterialColor.Red._700.asColor,
                MaterialColor.Red._800.asColor,
                MaterialColor.Red._900.asColor,
                MaterialColor.Red._A100.asColor,
                MaterialColor.Red._A200.asColor,
                MaterialColor.Red._A400.asColor,
                MaterialColor.Red._A700.asColor
            ), intArrayOf(
                MaterialColor.Pink._50.asColor,
                MaterialColor.Pink._100.asColor,
                MaterialColor.Pink._200.asColor,
                MaterialColor.Pink._300.asColor,
                MaterialColor.Pink._400.asColor,
                MaterialColor.Pink._500.asColor,
                MaterialColor.Pink._600.asColor,
                MaterialColor.Pink._700.asColor,
                MaterialColor.Pink._800.asColor,
                MaterialColor.Pink._900.asColor,
                MaterialColor.Pink._A100.asColor,
                MaterialColor.Pink._A200.asColor,
                MaterialColor.Pink._A400.asColor,
                MaterialColor.Pink._A700.asColor
            ), intArrayOf(
                MaterialColor.Purple._50.asColor,
                MaterialColor.Purple._100.asColor,
                MaterialColor.Purple._200.asColor,
                MaterialColor.Purple._300.asColor,
                MaterialColor.Purple._400.asColor,
                MaterialColor.Purple._500.asColor,
                MaterialColor.Purple._600.asColor,
                MaterialColor.Purple._700.asColor,
                MaterialColor.Purple._800.asColor,
                MaterialColor.Purple._900.asColor,
                MaterialColor.Purple._A100.asColor,
                MaterialColor.Purple._A200.asColor,
                MaterialColor.Purple._A400.asColor,
                MaterialColor.Purple._A700.asColor
            ), intArrayOf(
                MaterialColor.DeepPurple._50.asColor,
                MaterialColor.DeepPurple._100.asColor,
                MaterialColor.DeepPurple._200.asColor,
                MaterialColor.DeepPurple._300.asColor,
                MaterialColor.DeepPurple._400.asColor,
                MaterialColor.DeepPurple._500.asColor,
                MaterialColor.DeepPurple._600.asColor,
                MaterialColor.DeepPurple._700.asColor,
                MaterialColor.DeepPurple._800.asColor,
                MaterialColor.DeepPurple._900.asColor,
                MaterialColor.DeepPurple._A100.asColor,
                MaterialColor.DeepPurple._A200.asColor,
                MaterialColor.DeepPurple._A400.asColor,
                MaterialColor.DeepPurple._A700.asColor
            ), intArrayOf(
                MaterialColor.Indigo._50.asColor,
                MaterialColor.Indigo._100.asColor,
                MaterialColor.Indigo._200.asColor,
                MaterialColor.Indigo._300.asColor,
                MaterialColor.Indigo._400.asColor,
                MaterialColor.Indigo._500.asColor,
                MaterialColor.Indigo._600.asColor,
                MaterialColor.Indigo._700.asColor,
                MaterialColor.Indigo._800.asColor,
                MaterialColor.Indigo._900.asColor,
                MaterialColor.Indigo._A100.asColor,
                MaterialColor.Indigo._A200.asColor,
                MaterialColor.Indigo._A400.asColor,
                MaterialColor.Indigo._A700.asColor
            ), intArrayOf(
                MaterialColor.Blue._50.asColor,
                MaterialColor.Blue._100.asColor,
                MaterialColor.Blue._200.asColor,
                MaterialColor.Blue._300.asColor,
                MaterialColor.Blue._400.asColor,
                MaterialColor.Blue._500.asColor,
                MaterialColor.Blue._600.asColor,
                MaterialColor.Blue._700.asColor,
                MaterialColor.Blue._800.asColor,
                MaterialColor.Blue._900.asColor,
                MaterialColor.Blue._A100.asColor,
                MaterialColor.Blue._A200.asColor,
                MaterialColor.Blue._A400.asColor,
                MaterialColor.Blue._A700.asColor
            ), intArrayOf(
                MaterialColor.LightBlue._50.asColor,
                MaterialColor.LightBlue._100.asColor,
                MaterialColor.LightBlue._200.asColor,
                MaterialColor.LightBlue._300.asColor,
                MaterialColor.LightBlue._400.asColor,
                MaterialColor.LightBlue._500.asColor,
                MaterialColor.LightBlue._600.asColor,
                MaterialColor.LightBlue._700.asColor,
                MaterialColor.LightBlue._800.asColor,
                MaterialColor.LightBlue._900.asColor,
                MaterialColor.LightBlue._A100.asColor,
                MaterialColor.LightBlue._A200.asColor,
                MaterialColor.LightBlue._A400.asColor,
                MaterialColor.LightBlue._A700.asColor
            ), intArrayOf(
                MaterialColor.Cyan._50.asColor,
                MaterialColor.Cyan._100.asColor,
                MaterialColor.Cyan._200.asColor,
                MaterialColor.Cyan._300.asColor,
                MaterialColor.Cyan._400.asColor,
                MaterialColor.Cyan._500.asColor,
                MaterialColor.Cyan._600.asColor,
                MaterialColor.Cyan._700.asColor,
                MaterialColor.Cyan._800.asColor,
                MaterialColor.Cyan._900.asColor,
                MaterialColor.Cyan._A100.asColor,
                MaterialColor.Cyan._A200.asColor,
                MaterialColor.Cyan._A400.asColor,
                MaterialColor.Cyan._A700.asColor
            ), intArrayOf(
                MaterialColor.Teal._50.asColor,
                MaterialColor.Teal._100.asColor,
                MaterialColor.Teal._200.asColor,
                MaterialColor.Teal._300.asColor,
                MaterialColor.Teal._400.asColor,
                MaterialColor.Teal._500.asColor,
                MaterialColor.Teal._600.asColor,
                MaterialColor.Teal._700.asColor,
                MaterialColor.Teal._800.asColor,
                MaterialColor.Teal._900.asColor,
                MaterialColor.Teal._A100.asColor,
                MaterialColor.Teal._A200.asColor,
                MaterialColor.Teal._A400.asColor,
                MaterialColor.Teal._A700.asColor
            ), intArrayOf(
                MaterialColor.Green._50.asColor,
                MaterialColor.Green._100.asColor,
                MaterialColor.Green._200.asColor,
                MaterialColor.Green._300.asColor,
                MaterialColor.Green._400.asColor,
                MaterialColor.Green._500.asColor,
                MaterialColor.Green._600.asColor,
                MaterialColor.Green._700.asColor,
                MaterialColor.Green._800.asColor,
                MaterialColor.Green._900.asColor,
                MaterialColor.Green._A100.asColor,
                MaterialColor.Green._A200.asColor,
                MaterialColor.Green._A400.asColor,
                MaterialColor.Green._A700.asColor
            ), intArrayOf(
                MaterialColor.LightGreen._50.asColor,
                MaterialColor.LightGreen._100.asColor,
                MaterialColor.LightGreen._200.asColor,
                MaterialColor.LightGreen._300.asColor,
                MaterialColor.LightGreen._400.asColor,
                MaterialColor.LightGreen._500.asColor,
                MaterialColor.LightGreen._600.asColor,
                MaterialColor.LightGreen._700.asColor,
                MaterialColor.LightGreen._800.asColor,
                MaterialColor.LightGreen._900.asColor,
                MaterialColor.LightGreen._A100.asColor,
                MaterialColor.LightGreen._A200.asColor,
                MaterialColor.LightGreen._A400.asColor,
                MaterialColor.LightGreen._A700.asColor
            ), intArrayOf(
                MaterialColor.Lime._50.asColor,
                MaterialColor.Lime._100.asColor,
                MaterialColor.Lime._200.asColor,
                MaterialColor.Lime._300.asColor,
                MaterialColor.Lime._400.asColor,
                MaterialColor.Lime._500.asColor,
                MaterialColor.Lime._600.asColor,
                MaterialColor.Lime._700.asColor,
                MaterialColor.Lime._800.asColor,
                MaterialColor.Lime._900.asColor,
                MaterialColor.Lime._A100.asColor,
                MaterialColor.Lime._A200.asColor,
                MaterialColor.Lime._A400.asColor,
                MaterialColor.Lime._A700.asColor
            ), intArrayOf(
                MaterialColor.Yellow._50.asColor,
                MaterialColor.Yellow._100.asColor,
                MaterialColor.Yellow._200.asColor,
                MaterialColor.Yellow._300.asColor,
                MaterialColor.Yellow._400.asColor,
                MaterialColor.Yellow._500.asColor,
                MaterialColor.Yellow._600.asColor,
                MaterialColor.Yellow._700.asColor,
                MaterialColor.Yellow._800.asColor,
                MaterialColor.Yellow._900.asColor,
                MaterialColor.Yellow._A100.asColor,
                MaterialColor.Yellow._A200.asColor,
                MaterialColor.Yellow._A400.asColor,
                MaterialColor.Yellow._A700.asColor
            ), intArrayOf(
                MaterialColor.Amber._50.asColor,
                MaterialColor.Amber._100.asColor,
                MaterialColor.Amber._200.asColor,
                MaterialColor.Amber._300.asColor,
                MaterialColor.Amber._400.asColor,
                MaterialColor.Amber._500.asColor,
                MaterialColor.Amber._600.asColor,
                MaterialColor.Amber._700.asColor,
                MaterialColor.Amber._800.asColor,
                MaterialColor.Amber._900.asColor,
                MaterialColor.Amber._A100.asColor,
                MaterialColor.Amber._A200.asColor,
                MaterialColor.Amber._A400.asColor,
                MaterialColor.Amber._A700.asColor
            ), intArrayOf(
                MaterialColor.Orange._50.asColor,
                MaterialColor.Orange._100.asColor,
                MaterialColor.Orange._200.asColor,
                MaterialColor.Orange._300.asColor,
                MaterialColor.Orange._400.asColor,
                MaterialColor.Orange._500.asColor,
                MaterialColor.Orange._600.asColor,
                MaterialColor.Orange._700.asColor,
                MaterialColor.Orange._800.asColor,
                MaterialColor.Orange._900.asColor,
                MaterialColor.Orange._A100.asColor,
                MaterialColor.Orange._A200.asColor,
                MaterialColor.Orange._A400.asColor,
                MaterialColor.Orange._A700.asColor
            ), intArrayOf(
                MaterialColor.DeepOrange._50.asColor,
                MaterialColor.DeepOrange._100.asColor,
                MaterialColor.DeepOrange._200.asColor,
                MaterialColor.DeepOrange._300.asColor,
                MaterialColor.DeepOrange._400.asColor,
                MaterialColor.DeepOrange._500.asColor,
                MaterialColor.DeepOrange._600.asColor,
                MaterialColor.DeepOrange._700.asColor,
                MaterialColor.DeepOrange._800.asColor,
                MaterialColor.DeepOrange._900.asColor,
                MaterialColor.DeepOrange._A100.asColor,
                MaterialColor.DeepOrange._A200.asColor,
                MaterialColor.DeepOrange._A400.asColor,
                MaterialColor.DeepOrange._A700.asColor
            ), intArrayOf(
                MaterialColor.Brown._50.asColor,
                MaterialColor.Brown._100.asColor,
                MaterialColor.Brown._200.asColor,
                MaterialColor.Brown._300.asColor,
                MaterialColor.Brown._400.asColor,
                MaterialColor.Brown._500.asColor,
                MaterialColor.Brown._600.asColor,
                MaterialColor.Brown._700.asColor,
                MaterialColor.Brown._800.asColor,
                MaterialColor.Brown._900.asColor
            ), intArrayOf(
                MaterialColor.BlueGrey._50.asColor,
                MaterialColor.BlueGrey._100.asColor,
                MaterialColor.BlueGrey._200.asColor,
                MaterialColor.BlueGrey._300.asColor,
                MaterialColor.BlueGrey._400.asColor,
                MaterialColor.BlueGrey._500.asColor,
                MaterialColor.BlueGrey._600.asColor,
                MaterialColor.BlueGrey._700.asColor,
                MaterialColor.BlueGrey._800.asColor,
                MaterialColor.BlueGrey._900.asColor
            ), intArrayOf(
                MaterialColor.White._1000.asColor,
                MaterialColor.Grey._50.asColor,
                MaterialColor.Grey._100.asColor,
                MaterialColor.Grey._200.asColor,
                MaterialColor.Grey._300.asColor,
                MaterialColor.Grey._400.asColor,
                MaterialColor.Grey._500.asColor,
                MaterialColor.Grey._600.asColor,
                MaterialColor.Grey._700.asColor,
                MaterialColor.Grey._800.asColor,
                MaterialColor.Grey._900.asColor,
                MaterialColor.Black._1000.asColor
            )

        )
}

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
            ACCENT1  -> when (deep) {
                DEPTH_0    -> context.getColor(android.R.color.system_accent1_0)
                DEPTH_10   -> context.getColor(android.R.color.system_accent1_10)
                DEPTH_50   -> context.getColor(android.R.color.system_accent1_50)
                DEPTH_100  -> context.getColor(android.R.color.system_accent1_100)
                DEPTH_200  -> context.getColor(android.R.color.system_accent1_200)
                DEPTH_300  -> context.getColor(android.R.color.system_accent1_300)
                DEPTH_400  -> context.getColor(android.R.color.system_accent1_400)
                DEPTH_500  -> context.getColor(android.R.color.system_accent1_500)
                DEPTH_600  -> context.getColor(android.R.color.system_accent1_600)
                DEPTH_700  -> context.getColor(android.R.color.system_accent1_700)
                DEPTH_800  -> context.getColor(android.R.color.system_accent1_800)
                DEPTH_900  -> context.getColor(android.R.color.system_accent1_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_accent1_1000)
                else       -> -1
            }

            ACCENT2  -> when (deep) {
                DEPTH_0    -> context.getColor(android.R.color.system_accent2_0)
                DEPTH_10   -> context.getColor(android.R.color.system_accent2_10)
                DEPTH_50   -> context.getColor(android.R.color.system_accent2_50)
                DEPTH_100  -> context.getColor(android.R.color.system_accent2_100)
                DEPTH_200  -> context.getColor(android.R.color.system_accent2_200)
                DEPTH_300  -> context.getColor(android.R.color.system_accent2_300)
                DEPTH_400  -> context.getColor(android.R.color.system_accent2_400)
                DEPTH_500  -> context.getColor(android.R.color.system_accent2_500)
                DEPTH_600  -> context.getColor(android.R.color.system_accent2_600)
                DEPTH_700  -> context.getColor(android.R.color.system_accent2_700)
                DEPTH_800  -> context.getColor(android.R.color.system_accent2_800)
                DEPTH_900  -> context.getColor(android.R.color.system_accent2_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_accent2_1000)
                else       -> -1
            }

            ACCENT3  -> when (deep) {
                DEPTH_0    -> context.getColor(android.R.color.system_accent3_0)
                DEPTH_10   -> context.getColor(android.R.color.system_accent3_10)
                DEPTH_50   -> context.getColor(android.R.color.system_accent3_50)
                DEPTH_100  -> context.getColor(android.R.color.system_accent3_100)
                DEPTH_200  -> context.getColor(android.R.color.system_accent3_200)
                DEPTH_300  -> context.getColor(android.R.color.system_accent3_300)
                DEPTH_400  -> context.getColor(android.R.color.system_accent3_400)
                DEPTH_500  -> context.getColor(android.R.color.system_accent3_500)
                DEPTH_600  -> context.getColor(android.R.color.system_accent3_600)
                DEPTH_700  -> context.getColor(android.R.color.system_accent3_700)
                DEPTH_800  -> context.getColor(android.R.color.system_accent3_800)
                DEPTH_900  -> context.getColor(android.R.color.system_accent3_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_accent3_1000)
                else       -> -1
            }

            NEUTRAL1 -> when (deep) {
                DEPTH_0    -> context.getColor(android.R.color.system_neutral1_0)
                DEPTH_10   -> context.getColor(android.R.color.system_neutral1_10)
                DEPTH_50   -> context.getColor(android.R.color.system_neutral1_50)
                DEPTH_100  -> context.getColor(android.R.color.system_neutral1_100)
                DEPTH_200  -> context.getColor(android.R.color.system_neutral1_200)
                DEPTH_300  -> context.getColor(android.R.color.system_neutral1_300)
                DEPTH_400  -> context.getColor(android.R.color.system_neutral1_400)
                DEPTH_500  -> context.getColor(android.R.color.system_neutral1_500)
                DEPTH_600  -> context.getColor(android.R.color.system_neutral1_600)
                DEPTH_700  -> context.getColor(android.R.color.system_neutral1_700)
                DEPTH_800  -> context.getColor(android.R.color.system_neutral1_800)
                DEPTH_900  -> context.getColor(android.R.color.system_neutral1_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_neutral1_1000)
                else       -> -1
            }

            NEUTRAL2 -> when (deep) {
                DEPTH_0    -> context.getColor(android.R.color.system_neutral2_0)
                DEPTH_10   -> context.getColor(android.R.color.system_neutral2_10)
                DEPTH_50   -> context.getColor(android.R.color.system_neutral2_50)
                DEPTH_100  -> context.getColor(android.R.color.system_neutral2_100)
                DEPTH_200  -> context.getColor(android.R.color.system_neutral2_200)
                DEPTH_300  -> context.getColor(android.R.color.system_neutral2_300)
                DEPTH_400  -> context.getColor(android.R.color.system_neutral2_400)
                DEPTH_500  -> context.getColor(android.R.color.system_neutral2_500)
                DEPTH_600  -> context.getColor(android.R.color.system_neutral2_600)
                DEPTH_700  -> context.getColor(android.R.color.system_neutral2_700)
                DEPTH_800  -> context.getColor(android.R.color.system_neutral2_800)
                DEPTH_900  -> context.getColor(android.R.color.system_neutral2_900)
                DEPTH_1000 -> context.getColor(android.R.color.system_neutral2_1000)
                else       -> -1
            }

            else     -> -1
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