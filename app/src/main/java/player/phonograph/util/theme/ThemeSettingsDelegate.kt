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
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import android.content.Context
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
                THEME_AUTO_LIGHTBLACK -> if (systemNightMode(resources)) THEME_BLACK else THEME_LIGHT
                THEME_AUTO_LIGHTDARK  -> if (systemNightMode(resources)) THEME_DARK else THEME_LIGHT
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

    fun isNightTheme(resources: Resources): Boolean =
        when (_theme.value) {
            THEME_AUTO_LIGHTBLACK, THEME_AUTO_LIGHTDARK -> systemNightMode(resources)
            THEME_DARK, THEME_BLACK                     -> true
            else                                        -> false
        }

    private fun systemNightMode(resources: Resources): Boolean =
        systemNightMode(resources.configuration) ?: false

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
    //endregion

    //region Color

    private val _selectedPrimaryColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _selectedAccentColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _monetPalettePrimaryColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _monetPaletteAccentColor: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val _enableMonet: MutableStateFlow<Int> = MutableStateFlow(0)

    val primaryColor: Flow<Int> =
        combine(_enableMonet, _monetPalettePrimaryColor, _selectedPrimaryColor) { (enabled, monet, selected) ->
            if (enabled > 0) monet else selected
        }

    val accentColor: Flow<Int> =
        combine(_enableMonet, _monetPaletteAccentColor, _selectedAccentColor) { (enabled, monet, selected) ->
            if (enabled > 0) monet else selected
        }


    @ColorInt
    fun primaryColor(): Int =
        if (_enableMonet.value > 0) {
            _monetPalettePrimaryColor.value
        } else {
            _selectedPrimaryColor.value
        }

    @ColorInt
    fun accentColor(): Int =
        if (_enableMonet.value > 0) {
            _monetPaletteAccentColor.value
        } else {
            _selectedAccentColor.value
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
        jobs += observeSetting(context, Keys.theme, scope) {
            _theme.value = it
        }
        jobs += observeSetting(context, Keys.selectedPrimaryColor, scope) {
            _selectedPrimaryColor.value = it
        }
        jobs += observeSetting(context, Keys.monetPalettePrimaryColor, scope) {
            _monetPalettePrimaryColor.value = it
        }
        jobs += observeSetting(context, Keys.selectedAccentColor, scope) {
            _selectedAccentColor.value = it
        }
        jobs += observeSetting(context, Keys.monetPaletteAccentColor, scope) {
            _monetPaletteAccentColor.value = it
        }
        jobs += observeSetting(context, Keys.enableMonet, scope) {
            _enableMonet.value = if (it) 1 else -1
        }

    }

    fun stopObserve() {
        while (jobs.isNotEmpty()) {
            jobs.removeAt(0).cancel(CancellationException())
        }
    }
    //endregion


}