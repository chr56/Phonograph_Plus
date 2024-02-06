/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import util.phonograph.tagsources.util.JsonDeserializationRetrofitConverter
import okhttp3.internal.format
import util.phonograph.tagsources.AbsRestClient
import android.content.Context

class LastFMRestClient(context: Context, userAgent: String) : AbsRestClient<LastFMService>(
    okHttpClientConfig = {
        defaultCache(context, "/okhttp-lastfm/")
    },
    headerConfig = {
        addHeader("Cache-Control", format("max-age=%d, max-stale=%d", 31536000, 31536000))
    },
    retrofitConfig = {
        baseUrl(BASE_URL)
        addConverterFactory(JsonDeserializationRetrofitConverter.Factory())
    },
    apiServiceClazz = LastFMService::class.java,
    customUserAgent = userAgent
) {
    companion object {
        const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
    }
}
