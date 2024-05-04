/*
 *  Copyright (c) 2022~2024 chr_56
 */

@file:JvmName("ThemeColorKt.kt")

package lib.phonograph.theme

import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import android.content.Context


@CheckResult
@ColorInt
fun Context.primaryColor(): Int = ThemeColor.primaryColor(this)


@CheckResult
@ColorInt
fun Context.accentColor(): Int = ThemeColor.accentColor(this)
