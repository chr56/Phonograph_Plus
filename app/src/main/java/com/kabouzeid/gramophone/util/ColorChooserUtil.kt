package com.kabouzeid.gramophone.util
import android.content.Context
import android.os.Build
import androidx.preference.Preference
import chr_56.MDthemer.color.MaterialColor
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.appshortcuts.DynamicShortcutManager

class ColorChooserListener(context: Context, defautColor: Int, mode: Int) : Preference.OnPreferenceClickListener {
    private var context: Context
    private var defaultColor: Int = 0
    private var mode: Int = 0
//    private val PRIMANY_COLOR: Int = 1
//    private val ACCENT_COLOR: Int = 2

    init {
        this.defaultColor = defautColor
        this.context = context
        this.mode = mode
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        val dialog = MaterialDialog(this.context)
            .title(R.string.pref_header_colors)
            .colorChooser(colors = colors, allowCustomArgb = true, initialSelection = defaultColor) {
                dialog, color ->
                val editor = ThemeColor.editTheme(context)
                when (mode) {
                    PRIMANY_COLOR -> editor.primaryColor(color)
                    ACCENT_COLOR -> editor.accentColor(color)
                    0 -> return@colorChooser
                }
                editor.commit()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    DynamicShortcutManager(context).updateDynamicShortcuts()
                }
            }
            .show()

        return true
    }

    val colors: IntArray = intArrayOf(
        MaterialColor.Red._A100.asColor,
        MaterialColor.Pink._A100.asColor,
        MaterialColor.DeepPurple._A100.asColor,
        MaterialColor.Indigo._A100.asColor,
        MaterialColor.Blue._A100.asColor,
        MaterialColor.LightBlue._A100.asColor,
        MaterialColor.Cyan._A100.asColor,
        MaterialColor.Teal._A100.asColor,
        MaterialColor.Green._A100.asColor,
        MaterialColor.LightGreen._A100.asColor,
        MaterialColor.Lime._A100.asColor,
        MaterialColor.Yellow._A100.asColor,
        MaterialColor.Amber._A100.asColor,
        MaterialColor.Orange._A100.asColor,
        MaterialColor.DeepOrange._A100.asColor,
        MaterialColor.BlueGrey._600.asColor,
        MaterialColor.Grey._600.asColor
    )
//    val subColors = listOf<IntArray>(
// TODO Colors
//
//    )
    companion object {
        val PRIMANY_COLOR: Int = 1
        val ACCENT_COLOR: Int = 2
    }
}
