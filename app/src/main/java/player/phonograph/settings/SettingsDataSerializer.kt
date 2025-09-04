/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.settings

import player.phonograph.foundation.error.warning
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import android.content.Context
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class SettingsDataSerializer(context: Context) {
    private val context = context.applicationContext

    private fun serializedValue(obj: Any?): JsonElement = when (obj) {
        is String  -> JsonPrimitive("$SEP$TS$SEP$obj")
        is Boolean -> JsonPrimitive("$SEP$TB$SEP$obj")
        is Int     -> JsonPrimitive("$SEP$TI$SEP$obj")
        is Long    -> JsonPrimitive("$SEP$TL$SEP$obj")
        is Float   -> JsonPrimitive("$SEP$TF$SEP$obj")
        is Set<*>  -> JsonArray(obj.map { JsonPrimitive("$SEP$TS$SEP$it") })
        null       -> JsonNull
        else       -> throw IllegalArgumentException("unsupported type: ${obj::class.java}")
    }

    private fun deserializeValue(key: String, raw: String): Preferences.Pair<out Any>? {
        if (!raw.startsWith(SEP)) {
            warning(context, TAG, "Setting `$key` is glitch: $raw")
            return null
        }
        val type = raw[1]
        val data = raw.substring(3)
        val pair = when (type) {
            TB   -> booleanPreferencesKey(key) to data.toBoolean()
            TS   -> stringPreferencesKey(key) to data
            TI   -> intPreferencesKey(key) to data.toInt()
            TL   -> longPreferencesKey(key) to data.toLong()
            TF   -> floatPreferencesKey(key) to data.toFloat()
            else -> null
        }
        if (pair == null) {
            warning(context, TAG, "Unsupported type `$type` in setting item `$key`")
        }
        return pair
    }

    fun serialize(preferences: Map<Preferences.Key<*>, Any?>): JsonObject {
        return JsonObject(preferences.mapKeys { it.key.name }.mapValues { serializedValue(it.value) })
    }

    fun deserialize(elements: Map<String, JsonElement>): Array<Preferences.Pair<out Any>> =
        elements.mapNotNull { (jsonKey, jsonValue) ->
            when (jsonValue) {
                is JsonPrimitive -> deserializeValue(jsonKey, jsonValue.content)

                is JsonArray     -> {
                    val strings = jsonValue.mapNotNull {
                        (it as? JsonPrimitive)?.content?.substring(3)
                    }
                    stringSetPreferencesKey(jsonKey) to strings.toSet()
                }

                else             -> {
                    warning(context, TAG, "unexpected setting item `$jsonKey`: $jsonValue")
                    null
                }
            }
        }.toTypedArray()

    companion object {
        private const val SEP = '/'
        private const val TB = 'B'
        private const val TS = 'S'
        private const val TI = 'I'
        private const val TL = 'L'
        private const val TF = 'F'

        private const val TAG = "SettingsSerializer"
    }
}
