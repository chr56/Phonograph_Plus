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

    private val dataStore = context.dataStore

    val flow: Flow<T>
        get() = dataStore.data.map { it[key.preferenceKey] ?: key.defaultValue() }

    suspend fun flowData(): T = dataStore.data.first()[key.preferenceKey] ?: key.defaultValue()

    suspend fun edit(value: () -> T) {
        dataStore.edit { mutablePreferences ->
            mutablePreferences[key.preferenceKey] = value()
        }
    }

    val data: T get() = runBlocking { flowData() }

    fun put(value: T) = runBlocking { edit { value } }

}

class CompositePreference<T>(private val key: CompositeKey<T>, context: Context) : Preference<T> {

    private val dataStore = context.dataStore

    suspend fun flow(): Flow<T> = key.valueProvider.flow(dataStore)

    suspend fun flowData(): T = flow().first() ?: key.defaultValue()

    suspend fun edit(value: () -> T) {
        key.valueProvider.edit(dataStore, value)
    }

    val data: T get() = runBlocking { flowData() }

    fun put(value: T) = runBlocking { edit { value } }
}