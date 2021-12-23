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
import java.util.concurrent.TimeUnit

object Updater {
    /**
     * @param callback a callback that would be executed if there's newer version ()
     * @param force true if you want to execute callback no mater there is no newer version or automatic check is disabled
     */
    fun checkUpdate(callback: (Bundle) -> Unit, force: Boolean = false) {
        if (!force && !PreferenceUtil.getInstance(App.instance).checkUpgradeAtStartup) {
            Log.w(TAG, "ignore upgrade check!"); return
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .build()

        blockLock = false // unlock

        val requestGithub = Request.Builder()
            .url(requestUriGitHub).get().build()
        val requestBitBucket = Request.Builder()
            .url(requestUriBitBucket).get().build()
        val requestJsdelivr = Request.Builder()
            .url(requestUriJsdelivr).get().build()
        val requestFastGit = Request.Builder()
            .url(requestUriFastGit).get().build()

        sendRequest(
            okHttpClient, requestGithub,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock) handleResponse(callback, force, call, response) else logIgnored(call)
            }
        )
        sendRequest(
            okHttpClient, requestBitBucket,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock) handleResponse(callback, force, call, response) else logIgnored(call)
            }
        )
        sendRequest(
            okHttpClient, requestJsdelivr,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock) handleResponse(callback, force, call, response) else logIgnored(call)
            }
        )
        sendRequest(
            okHttpClient, requestFastGit,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock) handleResponse(callback, force, call, response) else logIgnored(call)
            }
        )
    }

    private inline fun sendRequest(
        client: OkHttpClient,
        request: Request,
        crossinline failureCallback: (Call, IOException) -> Unit,
        crossinline successCallback: (Call, Response) -> Unit
    ): Call {
        return client.newCall(request).apply {
            this.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) =
                    failureCallback(call, e)

                override fun onResponse(call: Call, response: Response) =
                    successCallback(call, response)
            })
        }
    }

    private fun logFails(call: Call) =
        Log.e(TAG, "Fail to check new version! callUri = ${call.request().url()}")

    private fun logIgnored(call: Call) =
        Log.i(TAG, "Succeed to check new version, but it was blocked by an early successful call! callUri = ${call.request().url()}")

    private fun logSucceed(call: Call) =
        Log.i(TAG, "Succeed to check new version! callUri = ${call.request().url()}")

    var blockLock: Boolean = false

    private fun handleResponse(callback: (Bundle) -> Unit, force: Boolean, call: Call, response: Response) {
        blockLock = true // block other successful call

        logSucceed(call)

        val responseBody = response.body() ?: return

        var versionJson: VersionJson? = null
        try {
            versionJson = Gson().fromJson<VersionJson>(responseBody.string(), VersionJson::class.java)
        } catch (e: Exception) {
            blockLock = false
            e.printStackTrace()
            Log.e(TAG, "Parse version.json fail!")
        }

        versionJson?.let { json: VersionJson ->
            Log.i(
                TAG, "versionCode: ${json.versionCode}, version: ${json.version}, logSummary: ${json.logSummary}"
            )

            val result = Bundle().also {
                it.putInt(VersionCode, json.versionCode)
                it.putString(Version, json.version)
                it.putString(LogSummary, json.logSummary)
            }

            when {
                json.versionCode > BuildConfig.VERSION_CODE -> {
                    Log.i(TAG, "updatable!")
                    result.putBoolean(Upgradable, true)
                    callback.invoke(result)
                }
                json.versionCode == BuildConfig.VERSION_CODE -> {
                    Log.i(TAG, "no update, latest version!")
                    if (force) {
                        result.putBoolean(Upgradable, false)
                        callback.invoke(result)
                    }
                }
                json.versionCode < BuildConfig.VERSION_CODE -> {
                    Log.w(TAG, "no update, version is newer than latest?")
                    if (force) {
                        result.putBoolean(Upgradable, false)
                        callback.invoke(result)
                    }
                }
            }
        }
    }

    @Keep
    class VersionJson {
        @JvmField
        @SerializedName(Version)
        var version: String? = ""

        @JvmField
        @SerializedName(VersionCode)
        var versionCode: Int = 0

        @JvmField
        @SerializedName(LogSummary)
        var logSummary: String? = ""
    }

    private const val owner = "chr56"
    private const val organization = "Phonograph-Plus"
    private const val repo = "Phonograph_Plus"
    private const val branch = "dev"
    private const val file = "version.json"

    private const val requestUriGitHub = "https://raw.githubusercontent.com/$owner/$repo/$branch/$file"
    private const val requestUriBitBucket = "https://bitbucket.org/$organization/$repo/raw/$branch/$file"

    private const val requestUriJsdelivr = "https://cdn.jsdelivr.net/gh/$owner/$repo@$branch/$file"
    private const val requestUriFastGit = "https://endpoint.fastgit.org/https://github.com/$owner/$repo/blob/$branch/$file"

    private const val TAG = "Updater"

    const val Version = "version"
    const val VersionCode = "versionCode"
    const val LogSummary = "logSummary"
    const val Upgradable = "upgradable"
}
