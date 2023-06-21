/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import player.phonograph.BuildConfig
import player.phonograph.UpdateConfig.requestUriBitBucket
import player.phonograph.UpdateConfig.requestUriCodeberg
import player.phonograph.UpdateConfig.requestUriFastGit
import player.phonograph.UpdateConfig.requestUriGitHub
import player.phonograph.UpdateConfig.requestUriJsdelivr
import player.phonograph.model.version.VersionCatalog
import player.phonograph.settings.Setting
import player.phonograph.util.NetworkUtil.invokeRequest
import player.phonograph.util.debug
import player.phonograph.util.text.dateText
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

object Update {

    /**
     * check update from repositories
     * @param force override [Setting.ignoreUpgradeDate]
     * @param callback execute if [VersionCatalog] is fetched successfully
     */
    suspend fun checkUpdate(
        force: Boolean = false,
        callback: suspend (versionCatalog: VersionCatalog, upgradable: Boolean) -> Unit,
    ) {
        val versionCatalog = fetchVersionCatalog()
        if (versionCatalog != null) {
            val upgradable = checkUpgradable(versionCatalog, force)
            callback(versionCatalog, upgradable)
        }
    }


}

/**
 * @return [VersionCatalog] or null (failure)
 */
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun fetchVersionCatalog(): VersionCatalog? {
    return withContext(Dispatchers.IO + SupervisorJob()) {
        // source
        val requestGithub = Request.Builder().url(requestUriGitHub).get().build()

        // mirrors
        val requestCodeberg = Request.Builder().url(requestUriCodeberg).get().build()
        val requestBitBucket = Request.Builder().url(requestUriBitBucket).get().build()
        val requestJsdelivr = Request.Builder().url(requestUriJsdelivr).get().build()
        val requestFastGit = Request.Builder().url(requestUriFastGit).get().build()

        // check source first
        sendRequest(requestGithub)?.let { response ->
            logSucceed(response.request.url)
            val result = processResponse(response)
            result?.let {
                canAccessGitHub = true
                return@withContext it
            }
        }
        val channelCodeberg = checkFromRequest(requestCodeberg)
        val channelBitBucket = checkFromRequest(requestBitBucket)
        val channelJsdelivr = checkFromRequest(requestJsdelivr)
        val channelFastGit = checkFromRequest(requestFastGit)
        // check the fastest mirror
        val result: VersionCatalog? = select {
            channelCodeberg.onReceiveCatching { it.getOrNull() }
            channelBitBucket.onReceiveCatching { it.getOrNull() }
            channelJsdelivr.onReceiveCatching { it.getOrNull() }
            channelFastGit.onReceiveCatching { it.getOrNull() }
            onTimeout(18000) {
                Log.i(TAG, "Timeout!")
                null
            }
        }
        return@withContext result
    }
}

@ExperimentalCoroutinesApi
private suspend fun checkFromRequest(request: Request): ReceiveChannel<VersionCatalog> = coroutineScope {
    produce {
        val response = sendRequest(request)
        if (response != null) {
            val versionCatalog = processResponse(response)
            val url = response.request.url
            if (versionCatalog != null) {
                logSucceed(url)
                send(versionCatalog)
            } else {
                logFails(url)
            }
        }
    }
}

private suspend fun sendRequest(source: Request): Response? {
    return try {
        invokeRequest(request = source)
    } catch (e: IOException) {
        logFails(source.url)
        null
    }
}

/**
 * handle response
 * @return resolved VersionCatalog
 */
private suspend fun processResponse(response: Response): VersionCatalog? {
    return withContext(Dispatchers.Default) {
        try {
            response.body?.use {
                parser.decodeFromString<VersionCatalog>(it.string())
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to process response from ${response.request.url}", e)
            null
        }
    }
}

/**
 * check if the current is outdated
 * @param force override [Setting.ignoreUpgradeDate]
 * @return true if new versions available
 */
@Suppress("KotlinConstantConditions")
private fun checkUpgradable(versionCatalog: VersionCatalog, force: Boolean): Boolean {

    val currentVersionCode = BuildConfig.VERSION_CODE

    // filter current channel & latest
    val versions = versionCatalog.channelVersions
    val latestVersion = versions.maxByOrNull { version -> version.versionCode }

    if (versions.isEmpty() || latestVersion == null) {
        Log.e(TAG, "VersionCatalog seems corrupted: $versionCatalog")
        return false
    }

    // check if ignored
    val ignoredDate = Setting.instance.ignoreUpgradeDate
    val latestVersionByTime = versionCatalog.currentLatestChannelVersionBy { it.date }
    if (ignoredDate >= latestVersionByTime.date && !force) {
        Log.d(TAG, "ignore this upgrade: ${latestVersionByTime.date}(${dateText(latestVersionByTime.date)})")
        return false
    }


    val latestVersionCode = latestVersion.versionCode
    return when {
        latestVersionCode > currentVersionCode -> {
            debug { Log.v(TAG, "updatable!") }
            true
        }
        latestVersionCode == currentVersionCode -> {
            debug { Log.v(TAG, "no update, latest version!") }
            false
        }
        latestVersionCode < currentVersionCode -> {
            debug { Log.w(TAG, "no update, version is newer than latest?") }
            false
        }
        else -> false
    }
}


var canAccessGitHub = false
    private set

private fun logFails(url: HttpUrl) = Log.w(TAG, "Failed to check new version from $url!")

private fun logSucceed(url: HttpUrl) = debug {
    Log.i(TAG, "Succeeded to check new version from $url!")
}

private val parser = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private const val TAG = "Update"
