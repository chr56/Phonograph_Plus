/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import androidx.datastore.preferences.core.edit
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

interface Preference<T>
class PrimitivePreference<T>(private val key: PrimitiveKey<T>, context: Context) : Preference<T> {

    private val dataStore = Setting.settingsDatastore(context)

    val flow: Flow<T>
        get() = dataStore.data.map { it[key.preferenceKey] ?: key.defaultValue() }

    suspend fun flowData(): T = dataStore.data.first()[key.preferenceKey] ?: key.defaultValue()

    suspend fun edit(value: () -> T) {
        dataStore.edit { mutablePreferences ->
            mutablePreferences[key.preferenceKey] = value()
        }
    }

    /**
     * block api
     */
    var data: T
        get() = runBlocking { flowData() }
        set(value) = runBlocking { edit { value } }

}

class CompositePreference<T>(key: CompositeKey<T>, context: Context) : Preference<T> {

    private val dataStore = Setting.settingsDatastore(context)

    private val provider = key.valueProvider

    suspend fun flow(): Flow<T> = provider.flow(dataStore)

    suspend fun flowData(): T = flow().first() ?: provider.default()

    suspend fun edit(value: () -> T) {
        provider.edit(dataStore, value)
    }

    /**
     * block api
     */
    var data: T
        get() = runBlocking { flowData() }
        set(value) = runBlocking { edit { value } }
}