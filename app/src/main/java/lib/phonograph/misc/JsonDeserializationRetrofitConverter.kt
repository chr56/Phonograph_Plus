/*
 *  Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.misc

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.Type

class JsonDeserializationRetrofitConverter<T : Any>(
    private val serializer: JsonSerializerDelegate<T>,
) : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T = serializer.fromResponseBody(value)

    class Factory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit,
        ): Converter<ResponseBody, *> {
            return JsonDeserializationRetrofitConverter(JsonSerializerDelegate(type, _json))
        }

        companion object {
            private val _json =
                Json {
                    ignoreUnknownKeys = true
                }
        }
    }

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
}
