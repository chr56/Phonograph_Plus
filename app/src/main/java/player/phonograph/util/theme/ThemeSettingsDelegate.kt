/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.R
import player.phonograph.model.ui.GeneralTheme
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_AUTO_LIGHTBLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_AUTO_LIGHTDARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_BLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_DARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_LIGHT
import player.phonograph.settings.Keys
import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import player.phonograph.util.ui.MonetColor
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException


object ThemeSettingsDelegate {

    //region Theme Style
    private val _theme: MutableStateFlow<String?> = MutableStateFlow(null)
    val theme: StateFlow<String?> get() = _theme.asStateFlow()

    fun underlyingTheme(resources: Resources): Flow<String> =
        theme.map {
            when (it) {
                THEME_AUTO_LIGHTBLACK -> if (isNightTheme(resources)) THEME_BLACK else THEME_LIGHT
                THEME_AUTO_LIGHTDARK  -> if (isNightTheme(resources)) THEME_DARK else THEME_LIGHT
                THEME_LIGHT           -> THEME_LIGHT
                THEME_BLACK           -> THEME_BLACK
                THEME_DARK            -> THEME_DARK
                else                  -> THEME_DARK// default
            }
        }

    fun isAutoTheme(): Boolean =
        when (_theme.value) {
            THEME_AUTO_LIGHTBLACK, THEME_AUTO_LIGHTDARK -> true
            else                                        -> false
        }

    private val isNightTheme: Boolean get() = _config?.let(::isNightTheme) ?: false

    fun isNightTheme(resources: Resources): Boolean = isNightTheme(resources.configuration)

    fun isNightTheme(configuration: Configuration): Boolean =
        when (_theme.value) {
            THEME_AUTO_LIGHTBLACK, THEME_AUTO_LIGHTDARK -> systemNightMode(configuration) ?: false
            THEME_DARK, THEME_BLACK                     -> true
            else                                        -> false
        }

    val styleRes: Flow<Int> = _theme.map { themeStyleRes(it) }

    fun styleRes(): Int = themeStyleRes(_theme.value)

    @StyleRes
    private fun themeStyleRes(@GeneralTheme theme: String?): Int = when (theme) {
        THEME_AUTO_LIGHTBLACK -> R.style.Theme_Phonograph_Auto_LightBlack
        THEME_AUTO_LIGHTDARK  -> R.style.Theme_Phonograph_Auto_LightDark
        THEME_LIGHT           -> R.style.Theme_Phonograph_Light
        THEME_BLACK           -> R.style.Theme_Phonograph_Black
        THEME_DARK            -> R.style.Theme_Phonograph_Dark
        else                  -> R.style.Theme_Phonograph_Auto_LightBlack
    }

    private var _config: Configuration? = null
    //endregion

    //region Color

    private val _selectedPrimaryColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _selectedAccentColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _selectedPrimaryColorForNight: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _selectedAccentColorForNight: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _monetPalettePrimaryColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _monetPaletteAccentColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _monetPalettePrimaryColorForNight: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _monetPaletteAccentColorForNight: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _enableMonet: MutableStateFlow<Int> = MutableStateFlow(0)
    private val _enableColorSchemeForNight: MutableStateFlow<Int> = MutableStateFlow(1)


    val selectedPrimaryColor get() = _selectedPrimaryColor.asStateFlow()
    val selectedAccentColor get() = _selectedAccentColor.asStateFlow()
    val selectedPrimaryColorForNight get() = _selectedPrimaryColorForNight.asStateFlow()
    val selectedAccentColorForNight get() = _selectedAccentColorForNight.asStateFlow()
    val monetPalettePrimaryColor get() = _monetPalettePrimaryColor.asStateFlow()
    val monetPaletteAccentColor get() = _monetPaletteAccentColor.asStateFlow()
    val monetPalettePrimaryColorForNight get() = _monetPalettePrimaryColorForNight.asStateFlow()
    val monetPaletteAccentColorForNight get() = _monetPaletteAccentColorForNight.asStateFlow()

    /**
     * The primary color (ColorInt value flow) should be used currently.
     */
    val primaryColor: Flow<Int> =
        combine(
            _enableColorSchemeForNight,
            _enableMonet,
            _monetPalettePrimaryColor,
            _monetPalettePrimaryColorForNight,
            _selectedPrimaryColor,
            _selectedPrimaryColorForNight,
        ) { values ->
            val enableDarkColors = values[0]
            val enableMonet = values[1]
            val monet = values[2]
            val monetDark = values[3]
            val selected = values[4]
            val selectedDark = values[5]
            resolveColor(
                enableMonet = enableMonet > 0,
                enableDarkColors = enableDarkColors > 0,
                selectedColor = selected,
                selectedColorForNight = selectedDark,
                monetColor = monet,
                monetColorForNight = monetDark,
            )
        }

    /**
     * The accent color (ColorInt value flow) should be used currently.
     */
    val accentColor: Flow<Int> =
        combine(
            _enableColorSchemeForNight,
            _enableMonet,
            _monetPaletteAccentColor,
            _monetPaletteAccentColorForNight,
            _selectedAccentColor,
            _selectedAccentColorForNight,
        ) { values ->
            val enableDarkColors = values[0]
            val enableMonet = values[1]
            val monet = values[2]
            val monetDark = values[3]
            val selected = values[4]
            val selectedDark = values[5]
            resolveColor(
                enableMonet = enableMonet > 0,
                enableDarkColors = enableDarkColors > 0,
                selectedColor = selected,
                selectedColorForNight = selectedDark,
                monetColor = monet,
                monetColorForNight = monetDark,
            )
        }

    /**
     * The primary color (ColorInt value) should be used currently.
     */
    @ColorInt
    fun primaryColor(): Int = resolveColor(
        enableMonet = _enableMonet.value > 0,
        enableDarkColors = _enableColorSchemeForNight.value > 0,
        selectedColor = _selectedPrimaryColor.value,
        selectedColorForNight = _selectedPrimaryColorForNight.value,
        monetColor = _monetPalettePrimaryColor.value,
        monetColorForNight = _monetPalettePrimaryColorForNight.value,
    )

    /**
     * The accent color (ColorInt value) should be used currently.
     */
    @ColorInt
    fun accentColor(): Int = resolveColor(
        enableMonet = _enableMonet.value > 0,
        enableDarkColors = _enableColorSchemeForNight.value > 0,
        selectedColor = _selectedAccentColor.value,
        selectedColorForNight = _selectedAccentColorForNight.value,
        monetColor = _monetPaletteAccentColor.value,
        monetColorForNight = _monetPaletteAccentColorForNight.value,
    )

    @ColorInt
    private fun resolveColor(
        enableMonet: Boolean,
        enableDarkColors: Boolean,
        selectedColor: Int,
        selectedColorForNight: Int,
        monetColor: Int,
        monetColorForNight: Int,
    ): Int {
        val useDarkPalette = enableDarkColors && isNightTheme
        return if (enableMonet) {
            if (useDarkPalette) monetColorForNight else monetColor
        } else {
            if (useDarkPalette) selectedColorForNight else selectedColor
        }
    }

    //endregion

    //region Observe
    private fun <T> observeSetting(
        context: Context,
        key: PrimitiveKey<T>,
        scope: CoroutineScope,
        collector: FlowCollector<T>,
    ) = scope.launch(Dispatchers.IO) {
        Setting(context)[key].flow.distinctUntilChanged().collect(collector)
    }

    private var jobs = mutableListOf<Job>()

    fun startObserve(context: Context, scope: CoroutineScope) {
        _config = context.applicationContext.resources.configuration
        jobs += observeSetting(context, Keys.theme, scope) {
            _theme.value = it
        }
        jobs += observeSetting(context, Keys.selectedPrimaryColor, scope) {
            _selectedPrimaryColor.value = it
        }
        jobs += observeSetting(context, Keys.selectedPrimaryColorForNight, scope) {
            _selectedPrimaryColorForNight.value = it
        }
        jobs += observeSetting(context, Keys.monetPalettePrimaryColor, scope) {
            _monetPalettePrimaryColor.value = MonetColor.MonetColorPalette(it).color(context)
        }
        jobs += observeSetting(context, Keys.monetPalettePrimaryColorForNight, scope) {
            _monetPalettePrimaryColorForNight.value = MonetColor.MonetColorPalette(it).color(context)
        }
        jobs += observeSetting(context, Keys.selectedAccentColor, scope) {
            _selectedAccentColor.value = it
        }
        jobs += observeSetting(context, Keys.selectedAccentColorForNight, scope) {
            _selectedAccentColorForNight.value = it
        }
        jobs += observeSetting(context, Keys.monetPaletteAccentColor, scope) {
            _monetPaletteAccentColor.value = MonetColor.MonetColorPalette(it).color(context)
        }
        jobs += observeSetting(context, Keys.monetPaletteAccentColorForNight, scope) {
            _monetPaletteAccentColorForNight.value = MonetColor.MonetColorPalette(it).color(context)
        }
        jobs += observeSetting(context, Keys.enableMonet, scope) {
            _enableMonet.value = if (it) 1 else -1
        }
        jobs += observeSetting(context, Keys.enableColorSchemeForNight, scope) {
            _enableColorSchemeForNight.value = if (it) 1 else -1
        }

    }

    fun onConfigurationChanged(newConfig: Configuration) {
        _config = newConfig
    }

    fun stopObserve() {
        while (jobs.isNotEmpty()) {
            jobs.removeAt(0).cancel(CancellationException())
        }
    }
    //endregion


}
