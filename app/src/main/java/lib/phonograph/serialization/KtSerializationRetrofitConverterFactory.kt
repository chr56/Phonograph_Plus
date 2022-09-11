/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.serialization

import java.lang.reflect.Type
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

class DeserializationConverter<T : Any>(
    private val serializer: KtSerializationJsonSerializerDelegate<T>,
) : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T? {
        return serializer.fromResponseBody(serializer.serializer, value)
    }
}

class SerializationConverter<T : Any>(
    private val serializer: KtSerializationJsonSerializerDelegate<T>,
    private val contentType: MediaType,
) : Converter<T, RequestBody> {
    override fun convert(value: T): RequestBody {
        return serializer.toRequestBody(contentType, serializer.serializer, value)
    }
}


class KtSerializationRetrofitConverterFactory(
    private val contentType: MediaType,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        return DeserializationConverter(KtSerializationJsonSerializerDelegate(type))
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        return SerializationConverter(KtSerializationJsonSerializerDelegate(type), contentType)
    }
}
