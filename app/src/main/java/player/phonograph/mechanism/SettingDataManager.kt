/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import okio.BufferedSink
import okio.Source
import okio.buffer
import player.phonograph.BuildConfig.VERSION_CODE
import player.phonograph.model.backup.ExportedSetting
import player.phonograph.settings.Setting
import player.phonograph.util.gitRevisionHash
import player.phonograph.util.reportError
import player.phonograph.util.warning
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object SettingDataManager {

    suspend fun exportSettings(sink: BufferedSink, context: Context): Boolean =
        try {
            val format = Json { prettyPrint = true }
            val preferences = Setting(context).dataStore.data.first().asMap()
            val content = SettingsSerializer(format, context).serialize(preferences)
            sink.writeString(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to convert SharedPreferences to Json")
            false
        }

    suspend fun importSettings(context: Context, source: Source): Boolean =
        try {
            val format = Json { prettyPrint = true }

            val rawString = source.buffer().use { bufferedSource -> bufferedSource.readUtf8() }

            val rawJson = format.decodeFromString<ExportedSetting>(rawString)

            val json = rawJson.content
            if (rawJson.formatVersion < ExportedSetting.VERSION) {
                warning(TAG, "This file is using legacy format")
            }


            val content = try {
                SettingsSerializer(format, context).deserialize(json)
            } catch (e: SerializationException) {
                reportError(e, TAG, "Failed to deserialize setting.")
                emptyArray()
            }
            Setting(context).dataStore.edit { preferences ->
                preferences.putAll(*content)
            }

            true
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to import Setting")
            false
        }

    class SettingsSerializer(private val format: StringFormat, context: Context) {

        private val appVersion = VERSION_CODE
        private val commitHash = gitRevisionHash(context)

        fun serialize(preferences: Map<Preferences.Key<*>, Any?>): String {
            fun serializedValue(obj: Any?): JsonElement = when (obj) {
                null       -> JsonNull
                is String  -> JsonPrimitive("$SEP$TS$SEP$obj")
                is Int     -> JsonPrimitive("$SEP$TI$SEP$obj")
                is Long    -> JsonPrimitive("$SEP$TL$SEP$obj")
                is Float   -> JsonPrimitive("$SEP$TF$SEP$obj")
                is Boolean -> JsonPrimitive("$SEP$TB$SEP$obj")
                is Set<*>  -> JsonArray(obj.map { JsonPrimitive("$SEP$TS$SEP$obj") })
                else       -> throw IllegalArgumentException("unsupported type")
            }

            val content = JsonObject(preferences.mapKeys { it.key.name }.mapValues { serializedValue(it.value) })
            val exported = ExportedSetting(
                formatVersion = ExportedSetting.VERSION,
                appVersion = appVersion,
                commitHash = commitHash,
                content = content
            )
            return format.encodeToString(exported)
        }

        fun deserialize(elements: Map<String, JsonElement>): Array<Preferences.Pair<out Any>> =
            elements.mapNotNull { (jsonKey, jsonValue) ->
                val v = (jsonValue as? JsonPrimitive)
                if (v != null) {
                    with(v) {
                        if (content.getOrNull(0) == SEP) {
                            val type = content[1]
                            val data = content.substring(3)
                            when (type) {
                                TB   -> booleanPreferencesKey(jsonKey) to data.toBoolean()
                                TS   -> stringPreferencesKey(jsonKey) to data
                                TI   -> intPreferencesKey(jsonKey) to data.toInt()
                                TL   -> longPreferencesKey(jsonKey) to data.toLong()
                                TF   -> floatPreferencesKey(jsonKey) to data.toFloat()
                                else -> throw IllegalStateException("unsupported type $type")
                            }
                        } else {
                            warning(TAG, "in key $jsonKey value $content is glitch")
                            null
                        }
                    }
                } else {
                    warning(TAG, "unexpected element")
                    null
                }
            }.toTypedArray()

        companion object {
            private const val SEP = '/'
            private const val TB = 'B'
            private const val TS = 'S'
            private const val TI = 'I'
            private const val TL = 'L'
            private const val TF = 'F'
        }
    }


    private const val TAG = "SettingDataManager"
}