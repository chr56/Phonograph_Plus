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
import androidx.annotation.Keep
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


@Keep
sealed class RestResult<T>(val isSuccess: Boolean) {

    fun dataOrNull() = (this as? Success)?.data
    fun messageOrNull() = (this as? RemoteError)?.message
    fun exceptionOrNull() = (this as? ParseError)?.exception

    class Success<T>(val data: T) : RestResult<T>(true)
    class RemoteError<T>(val message: String) : RestResult<T>(false)
    class ParseError<T>(val exception: Throwable) : RestResult<T>(false)
    class NetworkError<T>(val exception: Throwable) : RestResult<T>(false)
}

class JsonDeserializationRetrofitConverter<T : Any>(
    private val serializer: JsonSerializerDelegate<T>,
) : Converter<ResponseBody, RestResult<T>> {

    override fun convert(value: ResponseBody): RestResult<T> = serializer.fromResponseBody(value)

    class Factory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit,
        ): Converter<ResponseBody, *> {
            val wrappedType = (type as ParameterizedType).actualTypeArguments[0]
            return JsonDeserializationRetrofitConverter(JsonSerializerDelegate(wrappedType, _json))
        }

        companion object {
            private val _json =
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
        }
    }

    class JsonSerializerDelegate<T>(type: Type, private val _json: Json) {

        @Suppress("UNCHECKED_CAST")
        private val serializer: KSerializer<T> = _json.serializersModule.serializer(type) as KSerializer<T>

        @OptIn(ExperimentalSerializationApi::class)
        fun fromResponseBody(body: ResponseBody): RestResult<T> {
            val jsonString = body.string()
            var jsonElement: JsonElement? = null
            return try {
                jsonElement = _json.parseToJsonElement(jsonString)
                RestResult.Success(_json.decodeFromJsonElement(serializer, jsonElement))
            } catch (e: MissingFieldException) {
                // may return empty or error
                val jsonObject = jsonElement as? JsonObject
                val message = jsonObject?.get("message")
                if (message != null)
                    RestResult.RemoteError(message.toString())
                else
                    RestResult.ParseError(e)
            } catch (e: SerializationException) {
                RestResult.ParseError(e)
            }
        }

        fun toRequestBody(contentType: MediaType, value: T): RequestBody {
            val string = _json.encodeToString(serializer, value)
            return string.toRequestBody(contentType)
        }

    }
}
