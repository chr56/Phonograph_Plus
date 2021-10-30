/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object Updater {
    fun checkUpdate(context: Context) {
        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
            .url(uri)
            .get()
            .build()
        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, " check fail")
            }

            override fun onResponse(call: Call, response: Response) {
                val sources = response.body()?.string()
                Log.w(TAG, "success")

                val gson = Gson()
                val versionJson = gson.fromJson<VersionJson>(sources, VersionJson::class.java)
                Log.i(TAG, "versionCode: ${versionJson.versionCode}, version: ${versionJson.version}, logSummary: ${versionJson.logSummary}")

                if (versionJson.versionCode > BuildConfig.VERSION_CODE) {
                    Log.i(TAG, "updatable!")
                } else if (versionJson.versionCode == BuildConfig.VERSION_CODE) {
                    Log.i(TAG, "latest version!")
                } else {
                    Log.w(TAG, "version is newer than latest")
                }
            }
        })
    }
    class VersionJson {

        @SerializedName("version")
        var version: String? = ""
        @SerializedName("versionCode")
        var versionCode: Int = 0
        @SerializedName("logSummary")
        var logSummary: String? = ""
    }

    private const val base = "https://cdn.jsdelivr.net/gh/"
    private const val repo = "chr56/Phonograph_Plus@dev/"
    private const val file = "version.json"
    private const val uri = base + repo + file
    private const val TAG = "Updater"
}
