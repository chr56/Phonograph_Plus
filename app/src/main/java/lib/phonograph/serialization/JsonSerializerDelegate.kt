/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.serialization

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.Type

class JsonSerializerDelegate<T>(type: Type, private val _json: Json) {

    private val serializer: KSerializer<T> = _json.serializersModule.serializer(type) as KSerializer<T>

    fun fromResponseBody(body: ResponseBody): T {
        val jsonString = body.string()
        return _json.decodeFromString(serializer, jsonString)
    }

    fun toRequestBody(contentType: MediaType, value: T): RequestBody {
        val string = _json.encodeToString(serializer, value)
        return string.toRequestBody(contentType)
    }

}
