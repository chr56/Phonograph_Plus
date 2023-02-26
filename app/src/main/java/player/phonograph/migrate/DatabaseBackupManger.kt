/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate

import player.phonograph.provider.PathFilterStore
import player.phonograph.util.FileUtil.saveToFile
import player.phonograph.util.Util.reportError
import player.phonograph.util.Util.warning
import android.content.Context
import android.net.Uri
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.FileInputStream
import java.io.OutputStream

object DatabaseBackupManger {

    const val VERSION = "version"
    const val VERSION_CODE = 0


    private val parser by lazy(NONE) {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private const val WHITE_LIST = "whitelist"
    private const val BLACK_LIST = "blacklist"

    /**
     * @param destinationUri destination document uri
     */
    fun exportPathFilter(context: Context, destinationUri: Uri): Boolean {
        val json = exportPathFilter(context)
        val content = parser.encodeToString(json)
        saveToFile(destinationUri, content, context.contentResolver)
        return true
    }

    /**
     * close stream after use
     */
    fun exportPathFilter(context: Context, outputStream: OutputStream) {
        val json = exportPathFilter(context)
        val content = parser.encodeToString(json)
        outputStream.writer(Charsets.UTF_8).also {
            it.write(content)
            it.flush()
        }
    }

    private fun exportPathFilter(context: Context): JsonObject {
        val db = PathFilterStore.getInstance(context)
        val wl = db.whitelistPaths.map { JsonPrimitive(it) }
        val bl = db.blacklistPaths.map { JsonPrimitive(it) }
        return JsonObject(
            mapOf(
                VERSION to JsonPrimitive(VERSION_CODE),
                WHITE_LIST to JsonArray(wl),
                BLACK_LIST to JsonArray(bl),
            )
        )
    }

    fun importPathFilter(context: Context, sourceUri: Uri): Boolean {
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use {
            FileInputStream(it.fileDescriptor).use { fileInputStream ->
                val rawString: String = fileInputStream.use { stream ->
                    stream.bufferedReader().use { reader -> reader.readText() }
                }
                val json = parser.parseToJsonElement(rawString) as? JsonObject
                if (json != null) {
                    try {
                        importPathFilter(context, json, true)
                    } catch (e: Exception) {
                        reportError(e, TAG, "Failed!")
                    }
                } else {
                    warning(TAG, "Nothing to import")
                }
            }
        }
        return true
    }

    private fun importPathFilter(context: Context, json: JsonObject, override: Boolean) {

        val wl = json[WHITE_LIST] as? JsonArray
        val bl = json[BLACK_LIST] as? JsonArray


        val db = PathFilterStore.getInstance(context)

        if (!(wl == null && bl == null)) {

            bl?.map { (it as JsonPrimitive).content }?.let {
                if (override) db.clearBlacklist()
                db.addBlacklistPath(it)
            }

            wl?.map { (it as JsonPrimitive).content }?.let {
                if (override) db.clearWhitelist()
                db.addWhitelistPath(it)
            }

        } else {
            warning(TAG, "Nothing to import")
        }

    }

    private const val TAG = "DatabaseBackupManger"

}