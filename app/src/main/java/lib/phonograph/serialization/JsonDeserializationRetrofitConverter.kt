/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.serialization

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
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
}
