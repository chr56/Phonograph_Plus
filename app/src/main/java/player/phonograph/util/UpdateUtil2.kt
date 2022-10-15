/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import player.phonograph.BuildConfig
import player.phonograph.UpdateConfig2.requestUriBitBucket
import player.phonograph.UpdateConfig2.requestUriFastGit
import player.phonograph.UpdateConfig2.requestUriGitHub
import player.phonograph.UpdateConfig2.requestUriJsdelivr
import player.phonograph.misc.webRequest
import player.phonograph.model.version.VersionCatalog
import player.phonograph.settings.Setting
import player.phonograph.util.Util.debug
import java.io.IOException

object UpdateUtil2 {

    suspend fun checkUpdate(force: Boolean = false, callback: SuccessCallback) {
        val versionCatalog = checkVersionCatalog()
        if (versionCatalog != null) {
            val upgradable = checkUpgradable(versionCatalog, force)
            callback.onRequestSuccess(versionCatalog, upgradable)
        }
    }

    fun interface SuccessCallback {
        suspend fun onRequestSuccess(versionCatalog: VersionCatalog, upgradable: Boolean)
    }


    /**
     * @return version bundle or null (failure)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkVersionCatalog(): VersionCatalog? {
        return withContext(Dispatchers.IO + SupervisorJob()) {
            // source
            val requestGithub = Request.Builder()
                .url(requestUriGitHub).get().build()

            // mirrors
            val requestBitBucket = Request.Builder()
                .url(requestUriBitBucket).get().build()
            val requestJsdelivr = Request.Builder()
                .url(requestUriJsdelivr).get().build()
            val requestFastGit = Request.Builder()
                .url(requestUriFastGit).get().build()

            // check source first
            checkUpdate(requestGithub)?.let { response ->
                logSucceed(response.request.url)
                val result = process(response)
                result?.let { return@withContext it }
            }
            // check the fastest mirror
            val result = select<Response?> {
                produce {
                    send(checkUpdate(requestBitBucket))
                }
                produce {
                    send(checkUpdate(requestJsdelivr))
                }
                produce {
                    send(checkUpdate(requestFastGit))
                }
            }
            return@withContext if (result != null) {
                process(result)
            } else {
                null
            }
        }
    }

    /**
     * handle response
     * @return resolved VersionCatalog
     */
    private suspend fun process(response: Response): VersionCatalog? {
        return withContext(Dispatchers.Default) {
            try {
                response.body?.use {
                    parser.decodeFromString<VersionCatalog>(it.string())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * check if the current is outdated
     * @param force override [Setting.ignoreUpgradeVersionCode]
     * @return true if new versions available
     */
    @Suppress("KotlinConstantConditions")
    private fun checkUpgradable(versionCatalog: VersionCatalog, force: Boolean): Boolean {

        // // todo
        // val currentChannel = when (BuildConfig.FLAVOR) {
        //     "preview" -> "preview"
        //     "stable" -> "stable"
        //     else -> "stable"
        // }
        // val availableVersions =
        //     versionCatalog.versions.filter { it.channel == channel }

        val currentVersionCode = BuildConfig.VERSION_CODE
        val latestVersionCode = when (BuildConfig.FLAVOR) {
            "preview" -> versionCatalog.latestVersions.preview
            else -> versionCatalog.latestVersions.stable
        }

        // check if ignored
        val ignoreUpgradeVersionCode = Setting.instance.ignoreUpgradeVersionCode
        if (ignoreUpgradeVersionCode >= latestVersionCode && !force) {
            Log.d(TAG, "ignore this upgrade(version code: $latestVersionCode)")
            return false
        }


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

    private suspend fun checkUpdate(source: Request): Response? {
        return try {
            webRequest(request = source)
        } catch (e: IOException) {
            logFails(source.url)
            null
        }
    }


    private fun logFails(url: HttpUrl) =
        Log.w(TAG, "Failed to check new version from $url!")

    private fun logSucceed(url: HttpUrl) = debug {
        Log.i(TAG, "Succeeded to check new version from $url!")
    }

    private val parser = Json { ignoreUnknownKeys = true }

    private const val TAG = "UpdateUtil"

}

