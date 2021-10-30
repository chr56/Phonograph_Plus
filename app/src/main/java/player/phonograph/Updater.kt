/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph

import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object Updater {
    var result: Bundle? = null
    fun checkUpdate(/*callback: (Bundle) -> Unit*/) {
        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
            .url(uri)
            .get()
            .build()

        result = Bundle()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "check version fail")
            }

            override fun onResponse(call: Call, response: Response) {
                val sources = response.body()?.string()
                Log.w(TAG, "success")

                val gson = Gson()
                val versionJson = gson.fromJson<VersionJson>(sources, VersionJson::class.java)
                Log.i(TAG, "versionCode: ${versionJson.versionCode}, version: ${versionJson.version}, logSummary: ${versionJson.logSummary}")

                result?.let {
                    it.putInt(VersionCode, versionJson.versionCode)
                    it.putString(Version, versionJson.version)
                    it.putString(LogSummary, versionJson.logSummary)

                    if (versionJson.versionCode > BuildConfig.VERSION_CODE) {
                        Log.i(TAG, "updatable!")
                        it.putBoolean(Upgradable, true)
                    } else if (versionJson.versionCode == BuildConfig.VERSION_CODE) {
                        Log.i(TAG, "no update, latest version!")
                        it.putBoolean(Upgradable, false)
                    } else {
                        Log.w(TAG, "no update, version is newer than latest?")
                        it.putBoolean(Upgradable, false)
                    }
                }
            }
        })
    }

    class VersionJson {

        @SerializedName(Version)
        var version: String? = ""
        @SerializedName(VersionCode)
        var versionCode: Int = 0
        @SerializedName(LogSummary)
        var logSummary: String? = ""
    }

    private const val base = "https://cdn.jsdelivr.net/gh/"
    private const val repo = "chr56/Phonograph_Plus@dev/"
    private const val file = "version.json"
    private const val uri = base + repo + file
    private const val TAG = "Updater"

    const val Version = "version"
    const val VersionCode = "versionCode"
    const val LogSummary = "logSummary"
    const val Upgradable = "upgradable"
}
