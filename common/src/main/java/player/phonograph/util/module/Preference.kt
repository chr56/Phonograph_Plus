/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.module

import android.content.Context
import androidx.preference.PreferenceManager

class BooleanIsolatePreference(
    private val keyName: String,
    private val defValue: Boolean,
    context: Context
) : IsolatePreference<Boolean> {

    private val preference = PreferenceManager.getDefaultSharedPreferences(context)

    override fun read(): Boolean = preference.getBoolean(keyName, defValue)

    override fun write(value: Boolean) {
        preference.edit().apply {
            putBoolean(keyName, value)
        }.apply()
    }
}

class StringIsolatePreference(
    private val keyName: String,
    private val defValue: String?,
    context: Context
) : IsolatePreference<String?> {

    private val preference = PreferenceManager.getDefaultSharedPreferences(context)

    override fun read(): String? = preference.getString(keyName, defValue)

    override fun write(value: String?) {
        preference.edit().apply {
            putString(keyName, value)
        }.apply()
    }
}

sealed interface IsolatePreference<T> {
    fun read(): T
    fun write(value: T)
}
