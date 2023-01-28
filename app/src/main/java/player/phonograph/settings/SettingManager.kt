/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.settings

import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.BuildConfig.GIT_COMMIT_HASH
import player.phonograph.BuildConfig.VERSION_CODE
import player.phonograph.R
import player.phonograph.util.Util.reportError
import player.phonograph.util.Util.warning
import androidx.preference.PreferenceManager
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences.Editor
import android.net.Uri
import android.widget.Toast
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingManager(var context: Context) {

    private val parser by lazy(LazyThreadSafetyMode.NONE) { Json { prettyPrint = true } }

    fun exportSettings(uri: Uri): Boolean =
        try {
            val prefs = Setting.instance.rawMainPreference.all
            val model = serializedPref(prefs)
            val content = parser.encodeToString(model)
            saveToFile(uri, content, context.contentResolver)
            true
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to convert SharedPreferences to Json")
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

    private fun serializedPref(prefs: Map<String, Any?>): SettingExport {
        val content = JsonObject(
            prefs.mapValues { serializedValue(it.value) }
        )
        return SettingExport(
            VERSION,
            VERSION_CODE,
            GIT_COMMIT_HASH,
            content
        )
    }

    private fun serializedValue(obj: Any?): JsonElement = when (obj) {
        null       -> JsonNull
        is String  -> JsonPrimitive("${SEP}${TS}${SEP}$obj")
        is Int     -> JsonPrimitive("${SEP}${TI}${SEP}$obj")
        is Long    -> JsonPrimitive("${SEP}${TL}${SEP}$obj")
        is Float   -> JsonPrimitive("${SEP}${TF}${SEP}$obj")
        is Boolean -> JsonPrimitive("${SEP}${TB}${SEP}$obj")
        is Set<*>  -> JsonArray(obj.map { JsonPrimitive("${SEP}${TS}${SEP}$obj") })
        else       -> throw IllegalArgumentException("unsupported type")
    }


    private fun loadSettings(fileInputStream: FileInputStream): Boolean = try {

        val rawString: String = fileInputStream.use { stream ->
            stream.bufferedReader().use { it.readText() }
        }

        val rawJson = parser.decodeFromString<SettingExport>(rawString)

        val json = rawJson.content
        if (rawJson.formatVersion < VERSION) {
            warning(TAG, "This file is using legacy format")
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().let { editor ->
            for ((key, value) in json.entries) {
                deserializeValue(editor, key, value)
            }
            editor.apply()
        }
        true
    } catch (e: Exception) {
        reportError(e, TAG, "Failed to import Setting")
        false
    }

    private fun deserializeValue(editor: Editor, key: String, jsonElement: JsonElement) {
        when (jsonElement) {
            is JsonPrimitive -> {
                with(jsonElement) {
                    if (content.getOrNull(0) != SEP) {
                        if (jsonElement is JsonNull) {
                            editor.remove(key)
                        } else {
                            warning(TAG, "in key $key value $content is glitch")
                        }
                    } else {
                        val type = content[1]
                        val data = content.substring(3)
                        when (type) {
                            TB  -> editor.putBoolean(key, data.toBoolean())
                            TS  -> editor.putString(key, data)
                            TI  -> editor.putInt(key, data.toInt())
                            TL  -> editor.putLong(key, data.toLong())
                            TF  -> editor.putFloat(key, data.toFloat())
                            else -> warning(TAG, "unsupported type $type")
                        }
                    }
                }
            }
            is JsonArray     -> {
                val data = jsonElement.map { it.jsonPrimitive.content }
                editor.putStringSet(key, java.util.HashSet(data))
            }
            else             -> {
                warning(TAG, "unexpected element")
            }
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


    @kotlinx.serialization.Serializable
    private data class SettingExport(
        @SerialName("format_version") val formatVersion: Int,
        @SerialName("app_version") val appVersion: Int,
        @SerialName("commit_hash") val commitHash: String,
        @SerialName("content") val content: JsonObject
    )

    companion object {
        const val VERSION = 2
        private const val TAG = "SettingManager"

        private const val SEP = '/'
        private const val TB = 'B'
        private const val TS = 'S'
        private const val TI = 'I'
        private const val TL = 'L'
        private const val TF = 'F'

    }
}
