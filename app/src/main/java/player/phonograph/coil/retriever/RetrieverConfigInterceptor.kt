/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.retriever

import coil.intercept.Interceptor
import coil.request.ImageResult
import player.phonograph.coil.PARAMETERS_KEY_IMAGE_SOURCE_CONFIG

class RetrieverConfigInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {

        val config = collectSourceConfig(chain.request.context)

        val newRequest = chain.request.newBuilder()
            .setParameter(PARAMETERS_KEY_IMAGE_SOURCE_CONFIG, config)
            .build()

        return chain.proceed(newRequest)
    }
}