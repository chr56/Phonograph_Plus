/*
 *  Copyright (c) 2022~2023 chr_56, Karim Abou Zeid (kabouzeid)
 */

package util.phonograph.lastfm.rest

import lib.phonograph.serialization.KtSerializationRetrofitConverterFactory
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.internal.format
import okhttp3.internal.userAgent
import player.phonograph.BuildConfig
import retrofit2.Retrofit
import util.phonograph.lastfm.rest.service.LastFMService
import android.content.Context
import java.io.File

class LastFMRestClient private constructor(val apiService: LastFMService) {

    constructor(context: Context) : this(lastFMOkHttpClient(context).build())

    private constructor(client: Call.Factory) : this(restRetrofitAdapter(client))

    private constructor(retrofit: Retrofit) : this(retrofit.create(LastFMService::class.java))

    companion object {

        private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
        private const val USER_AGENT = "$userAgent PhonographPlus/${BuildConfig.VERSION_NAME}"

        private fun restRetrofitAdapter(client: Call.Factory): Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .callFactory(client)
            .addConverterFactory(KtSerializationRetrofitConverterFactory("application/json".toMediaType()))
            .build()

        private fun lastFMOkHttpClient(context: Context): OkHttpClient.Builder = OkHttpClient.Builder()
            .cache(defaultCache(context))
            .addInterceptor { chain: Interceptor.Chain ->
                val modifiedRequest = chain.request().newBuilder()
                    .addHeader("Cache-Control", format("max-age=%d, max-stale=%d", 31536000, 31536000))
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(modifiedRequest)
            }

        private fun defaultCache(context: Context): Cache? {
            val cacheDir = File(context.cacheDir.absolutePath, "/okhttp-lastfm/")
            return if (cacheDir.mkdirs() || cacheDir.isDirectory) Cache(cacheDir, 1024 * 1024 * 10) else null
        }
    }
}
