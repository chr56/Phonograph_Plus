/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.settings

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import java.io.*
import org.json.JSONArray
import org.json.JSONObject
import player.phonograph.notification.ErrorNotification

class SettingManager(var context: Context) {

    fun exportSettings(uri: Uri): Boolean {
        runCatching {
            val map = Setting.instance.export()
            val json = map.toJson()
            return@runCatching json.toString(2)
        }.let { r ->
            if (r.isFailure) ErrorNotification.postErrorNotification(
                r.exceptionOrNull() ?: Exception(),
                "Failed to convert SharedPreferences to Json"
            )else {
                saveToFile(uri, r.getOrNull()!!)
            }
            return r.isSuccess
        }
    }

    private fun saveToFile(dest: Uri, content: String) {
        context.contentResolver.openFileDescriptor(dest, "w")?.fileDescriptor?.let { fileDescriptor ->
            FileOutputStream(fileDescriptor).use { stream ->
                stream.bufferedWriter().apply {
                    write(content)
                    flush()
                    close()
                }
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

    private fun loadSettings(fileInputStream: FileInputStream): Boolean {
        runCatching {
            val json: JSONObject
            fileInputStream.use {
                it.bufferedReader().apply {
                    json = JSONObject(readText())
                    close()
                }
            }
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            for (key in json.keys()) {
                when (val value = json.opt(key)) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> {
                        if (value.isNotEmpty()) {
                            editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                        }
                    }
                    else -> throw IllegalArgumentException("value ${value.javaClass} is unsupported!")
                }
            }
            editor.apply()
        }.let { r ->
            if (r.isFailure) ErrorNotification.postErrorNotification(
                r.exceptionOrNull() ?: Exception(),
                "Failed to import Setting"
            )
            return r.isSuccess
        }
    }

    /**
     * Convert SharedPreferences map to Json Object
     */
    @Throws(IllegalArgumentException::class, IOException::class)
    internal fun Map<String, Any?>.toJson(): JSONObject =
        JSONObject().also { json ->
            val glitchList: MutableList<String> = ArrayList()
            for ((key, value) in this) {
                if (value == null) { glitchList.add(key); continue }
                when (value) {
                    is String -> {
                        json.put(key, value)
                    }
                    is Int -> {
                        json.put(key, value)
                    }
                    is Long -> {
                        json.put(key, value)
                    }
                    is Float -> {
                        json.put(key, value.toDouble())
                    }
                    is Boolean -> {
                        json.put(key, value)
                    }
                    is Set<*> -> {
                        json.put(
                            key,
                            JSONArray().also {
                                for (s in value) {
                                    it.put(s)
                                }
                            }
                        )
                    }
                    else -> {
                        val e = IllegalArgumentException("unsupported type")
                        ErrorNotification.postErrorNotification(
                            e, "Failed to convert SharedPreferences to Json: Unsupported type ${value.javaClass}"
                        )
                    }
                }
            }
            if (glitchList.isNotEmpty()) ErrorNotification.postErrorNotification(
                Throwable(),
                "Failed to save ${glitchList.reduce { acc, s: String -> acc + s }}"
            )
        }
}
