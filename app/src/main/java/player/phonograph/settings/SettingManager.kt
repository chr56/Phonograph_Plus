/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.settings

import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.util.Util.reportError
import androidx.preference.PreferenceManager
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences.Editor
import android.net.Uri
import android.widget.Toast
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingManager(var context: Context) {

    private val parser by lazy(LazyThreadSafetyMode.NONE) { Json { prettyPrint = true } }

    fun exportSettings(uri: Uri): Boolean =
        try {
            val prefs = Setting.instance.rawMainPreference.all
            val json: JsonElement = serializedPref(prefs)
            val content = parser.encodeToString(json)
            saveToFile(uri, content, context.contentResolver)
            true
        } catch (e: Exception) {
            reportError(e, "SettingManager", "Failed to convert SharedPreferences to Json")
            false
        }

    private fun saveToFile(dest: Uri, content: String, contentResolver: ContentResolver) {
        val fileDescriptor =
            contentResolver.openFileDescriptor(dest, "w")?.fileDescriptor

        FileOutputStream(fileDescriptor).use { stream ->
            stream.bufferedWriter().use {
                it.write(content)
                it.flush()
            }
        }
    }

    fun importSetting(uri: Uri): Boolean {
        return context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor?.let { fd ->
            FileInputStream(fd).use {
                loadSettings(it)
            }
        } ?: false
    }

    private fun serializedPref(prefs: Map<String, Any?>): JsonElement =
        JsonObject(
            prefs.mapValues { serializedValue(it.value) }
        )

    private fun serializedValue(obj: Any?): JsonElement = when (obj) {
        null       -> JsonNull
        is String  -> JsonPrimitive(obj)
        is Number  -> JsonPrimitive(obj)
        is Boolean -> JsonPrimitive(obj)
        is Set<*>  -> JsonArray(obj.map { serializedValue(it) })
        else       -> throw IllegalArgumentException("unsupported type")
    }


    private fun loadSettings(fileInputStream: FileInputStream): Boolean = try {

        val raw: String = fileInputStream.use { stream ->
            stream.bufferedReader().use { it.readText() }
        }

        val json = parser.parseToJsonElement(raw)

        PreferenceManager.getDefaultSharedPreferences(context).edit().let { editor ->
            for ((key, value) in (json as JsonObject).entries) {
                deserializeValue(editor, key, value)
            }
            editor.apply()
        }
        true
    } catch (e: Exception) {
        reportError(e, "SettingManager", "Failed to import Setting")
        false
    }

    private fun deserializeValue(editor: Editor, key: String, jsonElement: JsonElement) {
        // todo typesafe
        when (jsonElement) {
            is JsonPrimitive -> {
                with(jsonElement) {
                    when {
                        booleanOrNull != null -> editor.putBoolean(key, boolean)
                        intOrNull != null     -> editor.putInt(key, int)
                        longOrNull != null    -> editor.putLong(key, long)
                        floatOrNull != null   -> editor.putFloat(key, float)
                        isString              -> editor.putString(key, content)
                        else                  -> throw IllegalStateException("unsupported type")
                    }
                }
            }
            is JsonArray     -> {
                val data = jsonElement.map { it.jsonPrimitive.content }
                editor.putStringSet(key, java.util.HashSet(data))
            }
            is JsonNull      -> {}
            else             -> {}
        }
    }


    /**
     * **WARNING**! to reset all SharedPreferences!
     */
    @SuppressLint("ApplySharedPref") // must do immediately!
    fun clearAllPreference() {
        Setting.instance.rawMainPreference.edit().clear().commit()
        ThemeColor.editTheme(App.instance).clearAllPreference() // lib

        Toast.makeText(App.instance, R.string.success, Toast.LENGTH_SHORT).show()
    }
}
