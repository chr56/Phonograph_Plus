/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.musicbrainz

import lib.phonograph.misc.JsonDeserializationRetrofitConverter
import util.phonograph.tagsources.AbsRestClient
import android.content.Context

class MusicBrainzRestClient(context: Context, userAgent: String) : AbsRestClient<MusicBrainzService>(
    okHttpClientConfig = { defaultCache(context, "/okhttp-musicbrainz/") },
    headerConfig = {},
    retrofitConfig = {
        baseUrl(BASE_URL)
        addConverterFactory(JsonDeserializationRetrofitConverter.Factory())
    },
    apiServiceClazz = MusicBrainzService::class.java,
    customUserAgent = userAgent
) {
    companion object {
        const val BASE_URL = "https://musicbrainz.org/ws/2/"
    }
}