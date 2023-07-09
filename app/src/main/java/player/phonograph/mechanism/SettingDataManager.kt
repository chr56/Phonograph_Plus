/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import mt.pref.ThemeColor
import okio.BufferedSink
import player.phonograph.App
import player.phonograph.BuildConfig.VERSION_CODE
import player.phonograph.R
import player.phonograph.settings.dataStore
import player.phonograph.util.FileUtil.saveToFile
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
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.FileInputStream
import java.io.InputStream

object SettingDataManager {

    private val parser by lazy(NONE) { Json { prettyPrint = true } }

    suspend fun rawMainPreference(context: Context): Map<Preferences.Key<*>, Any> =
        context.dataStore.data.first().asMap()

    suspend fun exportSettings(uri: Uri, context: Context): Boolean =
        try {
            val prefs = rawMainPreference(context)
            val model = serializedPref(context, prefs)
            val content = parser.encodeToString(model)
            saveToFile(uri, content, context.contentResolver)
            true
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to convert SharedPreferences to Json")
            false
        }

    suspend fun exportSettings(sink: BufferedSink, context: Context): Boolean =
        try {
            val prefs = rawMainPreference(context)
            val model = serializedPref(context, prefs)
            val content = parser.encodeToString(model)
            sink.writeString(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to convert SharedPreferences to Json")
            false
        }

    fun importSetting(uri: Uri, context: Context): Boolean {
        return context.contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).use { stream ->
                loadSettings(stream, context)
            }
        } ?: false
    }

    fun importSetting(inputStream: InputStream, context: Context): Boolean =
        loadSettings(inputStream, context)

    private fun serializedPref(context: Context, prefs: Map<Preferences.Key<*>, Any?>): SettingExport {
        val content = JsonObject(
            prefs.mapKeys { it.key.name }.mapValues { serializedValue(it.value) }
        )
        return SettingExport(
            VERSION,
            VERSION_CODE,
            gitRevisionHash(context),
            content
        )
    }

    private fun serializedValue(obj: Any?): JsonElement = when (obj) {
        null       -> JsonNull
        is String  -> JsonPrimitive("$SEP$TS$SEP$obj")
        is Int     -> JsonPrimitive("$SEP$TI$SEP$obj")
        is Long    -> JsonPrimitive("$SEP$TL$SEP$obj")
        is Float   -> JsonPrimitive("$SEP$TF$SEP$obj")
        is Boolean -> JsonPrimitive("$SEP$TB$SEP$obj")
        is Set<*>  -> JsonArray(obj.map { JsonPrimitive("$SEP$TS$SEP$obj") })
        else       -> throw IllegalArgumentException("unsupported type")
    }


    private fun loadSettings(inputStream: InputStream, context: Context): Boolean = try {

        val rawString: String = inputStream.use { stream ->
            stream.bufferedReader().use { it.readText() }
        }

        val rawJson = parser.decodeFromString<SettingExport>(rawString)

        val json = rawJson.content
        if (rawJson.formatVersion < VERSION) {
            warning(TAG, "This file is using legacy format")
        }

        runBlocking {
            val prefArray = try {
                deserializeSettingJson(json)
            } catch (e: Exception) {
                reportError(e, TAG, "Failed to deserialize setting.")
                emptyArray()
            }
            context.dataStore.edit { preferences ->
                preferences.putAll(*prefArray)
            }
        }
        true
    } catch (e: Exception) {
        reportError(e, TAG, "Failed to import Setting")
        false
    }

    private fun deserializeSettingJson(elements: Map<String, JsonElement>): Array<Preferences.Pair<*>> =
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


    /**
     * **WARNING**! to reset all SharedPreferences!
     */
    @SuppressLint("ApplySharedPref") // must do immediately!
    fun clearAllPreference() {
        runBlocking {
            // todo forceUnregisterAllListener
            //Setting.instance.forceUnregisterAllListener()
            App.instance.dataStore.edit { it.clear() }
        }
        ThemeColor.editTheme(App.instance).clearAllPreference() // lib

        Toast.makeText(App.instance, R.string.success, Toast.LENGTH_SHORT).show()
    }


    @kotlinx.serialization.Serializable
    private data class SettingExport(
        @SerialName("format_version") val formatVersion: Int,
        @SerialName("app_version") val appVersion: Int,
        @SerialName("commit_hash") val commitHash: String,
        @SerialName("content") val content: JsonObject,
    )

    const val VERSION = 2
    private const val TAG = "SettingManager"

    private const val SEP = '/'
    private const val TB = 'B'
    private const val TS = 'S'
    private const val TI = 'I'
    private const val TL = 'L'
    private const val TF = 'F'

}
