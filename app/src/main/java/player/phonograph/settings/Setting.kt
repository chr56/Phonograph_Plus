/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.PreferenceManager
import player.phonograph.App
import player.phonograph.R
import util.mdcolor.pref.ThemeColor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Setting(context: Context) {

    private val mPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor: SharedPreferences.Editor = mPreferences.edit()

    fun registerOnSharedPreferenceChangedListener(sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        mPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun unregisterOnSharedPreferenceChangedListener(sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    /**
     * **WARNING**! to reset all SharedPreferences!
     */
    @SuppressLint("ApplySharedPref") // must do immediately!
    fun clearAllPreference() {
        editor.clear().commit()
        // lib
        ThemeColor.editTheme(App.instance).clearAllPreference()
        Toast.makeText(App.instance, R.string.success, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "Setting"

        private var singleton: Setting? = null
        val instance: Setting
            get() {
                if (singleton == null) singleton = Setting(App.instance)
                return singleton!!
            }

        /*
        @JvmStatic
        fun edit(): Setting {
            if (singleton == null) singleton = Setting(App.instance)
            return singleton!!
        }
        */
    }

    // Delegates

    inner class StringPref(private val keyName: String, private val defaultValue: String) : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String =
            mPreferences.getString(keyName, defaultValue) ?: defaultValue

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            editor.putString(keyName, value)
        }
    }

    inner class BooleanPref(private val keyName: String, private val defaultValue: Boolean) : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            mPreferences.getBoolean(keyName, defaultValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            editor.putBoolean(keyName, value)
        }
    }

    inner class IntPref(private val keyName: String, private val defaultValue: Int) : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            mPreferences.getInt(keyName, defaultValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            editor.putInt(keyName, value)
        }
    }

    inner class LongPref(private val keyName: String, private val defaultValue: Long) : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long =
            mPreferences.getLong(keyName, defaultValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            editor.putLong(keyName, value)
        }
    }
}
