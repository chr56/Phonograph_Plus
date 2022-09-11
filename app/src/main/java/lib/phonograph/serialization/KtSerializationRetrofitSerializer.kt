/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.serialization

import java.lang.reflect.Type
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody

class KtSerializationJsonSerializerDelegate<T : Any>(type: Type) {
    private val _json =
        Json {
            ignoreUnknownKeys = true
        }

    fun fromResponseBody(loader: DeserializationStrategy<T>, body: ResponseBody): T {
        val jsonString = body.string()
        return _json.decodeFromString(loader, jsonString)
    }

    fun toRequestBody(contentType: MediaType, saver: SerializationStrategy<T>, value: T): RequestBody {
        val string = _json.encodeToString(saver, value)
        return string.toRequestBody(contentType)
    }

    @OptIn(ExperimentalSerializationApi::class)
    val serializer: KSerializer<T> = _json.serializersModule.serializer(type) as KSerializer<T>
}
