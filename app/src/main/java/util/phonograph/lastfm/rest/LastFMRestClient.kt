package util.phonograph.lastfm.rest

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import util.phonograph.lastfm.rest.service.LastFMService
import java.io.File
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LastFMRestClient {

    val apiService: LastFMService

    constructor(f: okhttp3.Call.Factory) {
        val restAdapter = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .callFactory(f)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = restAdapter.create(LastFMService::class.java)
    }
    constructor(context: Context) : this(createDefaultOkHttpClientBuilder(context).build())

    companion object {

        private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"

        fun createDefaultCache(context: Context): Cache? {
            val cacheDir = File(context.cacheDir.absolutePath, "/okhttp-lastfm/")
            return if (cacheDir.mkdirs() || cacheDir.isDirectory) {
                Cache(cacheDir, 1024 * 1024 * 10)
            } else null
        }

        fun createCacheControlInterceptor(): Interceptor {
            return Interceptor { chain: Interceptor.Chain ->
                val modifiedRequest = chain.request().newBuilder()
                    .addHeader("Cache-Control", String.format(Locale.getDefault(), "max-age=%d, max-stale=%d", 31536000, 31536000))
                    .build()
                chain.proceed(modifiedRequest)
            }
        }

        fun createDefaultOkHttpClientBuilder(context: Context): OkHttpClient.Builder {
            return OkHttpClient.Builder()
                .cache(createDefaultCache(context))
                .addInterceptor(createCacheControlInterceptor())
        }
    }
}
