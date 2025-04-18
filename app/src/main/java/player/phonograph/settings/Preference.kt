/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

interface Preference<T> {

    /**
     *  an observable [Flow] of this preference
     */
    val flow: Flow<T>

    /**
     * read preference value
     */
    suspend fun read(): T

    /**
     * edit preference value
     */
    suspend fun edit(value: () -> T)


    /**
     * default value of this preference
     */
    val default: T

    /**
     * reset preference value
     */
    suspend fun reset() = edit { default }

    /**
     * read and write the preference in blocking way
     */
    var data: T
        get() = runBlocking { read() }
        set(value) = runBlocking { edit { value } }
}

class PrimitivePreference<T>(key: PrimitiveKey<T>, val dataStore: DataStore<Preferences>) : Preference<T> {

    private val preferenceKey: Preferences.Key<T> = key.preferenceKey
    private val defaultValue: () -> T = key.defaultValue

    override val flow: Flow<T> get() = dataStore.data.map { it[preferenceKey] ?: defaultValue() }

    override suspend fun read(): T = dataStore.data.first()[preferenceKey] ?: defaultValue()

    override suspend fun edit(value: () -> T) {
        dataStore.edit { mutablePreferences ->
            mutablePreferences[preferenceKey] = value()
        }
    }

    override val default: T get() = defaultValue()

}

class CompositePreference<T>(key: CompositeKey<T>, val dataStore: DataStore<Preferences>) : Preference<T> {

    private val provider = key.valueProvider

    override val flow: Flow<T> get() = provider.flow(dataStore)

    override suspend fun read(): T = flow.first() ?: provider.defaultValue()

    override suspend fun edit(value: () -> T) {
        provider.edit(dataStore, value)
    }

    override val default: T get() = provider.defaultValue()

}