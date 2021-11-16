/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph

import android.os.Bundle
import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import player.phonograph.util.PreferenceUtil
import java.io.IOException

object Updater {
    /**
     * @param callback a callback that would be executed if there's newer version ()
     * @param force true if you want to execute callback no mater there is no newer version or automatic check is disabled
     */
    fun checkUpdate(callback: (Bundle) -> Unit, force: Boolean = false) {
        if (!force && !PreferenceUtil.getInstance(App.instance).checkUpgradeAtStartup) { Log.w(TAG, "ignore upgrade check!") ; return }

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
            .url(requestUri)
            .get()
            .build()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Fail to check new version!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body() ?: return
                Log.i(TAG, "succeed to check new version!")

                val versionJson = Gson().fromJson<VersionJson>(responseBody.string(), VersionJson::class.java)
                Log.i(TAG, "versionCode: ${versionJson.versionCode}, version: ${versionJson.version}, logSummary: ${versionJson.logSummary}")

                val result = Bundle().also {
                    it.putInt(VersionCode, versionJson.versionCode)
                    it.putString(Version, versionJson.version)
                    it.putString(LogSummary, versionJson.logSummary)
                    it.putBoolean(Upgradable, false)
                }

                when {
                    versionJson.versionCode > BuildConfig.VERSION_CODE -> {
                        Log.i(TAG, "updatable!")
                        result.putBoolean(Upgradable, true)
                        callback.invoke(result)
                    }
                    versionJson.versionCode == BuildConfig.VERSION_CODE -> {
                        Log.i(TAG, "no update, latest version!")
                        if (force) {
                            result.putBoolean(Upgradable, false)
                            callback.invoke(result)
                        }
                    }
                    versionJson.versionCode < BuildConfig.VERSION_CODE -> {
                        Log.w(TAG, "no update, version is newer than latest?")
                        if (force) {
                            result.putBoolean(Upgradable, false)
                            callback.invoke(result)
                        }
                    }
                }
            }
        })
    }

    @Keep
    class VersionJson {
        @SerializedName(Version)
        var version: String? = ""
        @SerializedName(VersionCode)
        var versionCode: Int = 0
        @SerializedName(LogSummary)
        var logSummary: String? = ""
    }

    private const val owner = "chr56"
    private const val repo = "Phonograph_Plus"
    private const val branch = "dev"
    private const val file = "version.json"

    private const val requestUri = "https://cdn.jsdelivr.net/gh/$owner/$repo@$branch/$file"

    private const val TAG = "Updater"

    const val Version = "version"
    const val VersionCode = "versionCode"
    const val LogSummary = "logSummary"
    const val Upgradable = "upgradable"
}
