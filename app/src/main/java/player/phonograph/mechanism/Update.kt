/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import okhttp3.Request
import okhttp3.Response
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.foundation.notification.Notifications
import player.phonograph.model.version.VersionCatalog
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.util.NetworkUtil.invokeRequest
import player.phonograph.util.currentReleaseChannel
import player.phonograph.util.debug
import player.phonograph.util.text.dateText
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

object UpdateChecker {

    /**
     * check update from repositories
     * @param force override [Keys.ignoreUpgradeDate]
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

    fun sendNotification(
        context: Context,
        catalog: VersionCatalog,
        handlerIntent: Intent,
    ) {
        val version =
            catalog.versions.filter { it.channel == currentReleaseChannel }.maxByOrNull { it.versionCode } ?: return

        val title = version.versionName
        val note = version.releaseNote.parsed(context.resources)

        Notifications.Upgrade.post(
            context, title, note, handlerIntent
        )
    }

    /**
     * @return [VersionCatalog] or null (failure)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchVersionCatalog(): VersionCatalog? = withContext(Dispatchers.IO) {
        // source
        val requestGithub = Request.Builder().url(requestUriGitHub).get().build()

        // mirrors
        val requestCodeberg = Request.Builder().url(requestUriCodeberg).get().build()
        val requestBitBucket = Request.Builder().url(requestUriBitBucket).get().build()
        val requestJsdelivr = Request.Builder().url(requestUriJsdelivr).get().build()
        val requestFastGit = Request.Builder().url(requestUriFastGit).get().build()

        // check source first
        val channelGithub = checkFromRequest(requestGithub)
        channelGithub.receiveCatching().also { result ->
            val versionCatalog = result.getOrNull()
            if (versionCatalog != null) {
                canAccessGitHub = true
                return@withContext versionCatalog
            }
        }
        // check the fastest mirror
        val channelCodeberg = checkFromRequest(requestCodeberg)
        val channelBitBucket = checkFromRequest(requestBitBucket)
        val channelJsdelivr = checkFromRequest(requestJsdelivr)
        val channelFastGit = checkFromRequest(requestFastGit)
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

    @ExperimentalCoroutinesApi
    private suspend fun checkFromRequest(request: Request): ReceiveChannel<VersionCatalog> = coroutineScope {
        produce(capacity = 1) {
            val response = sendRequest(request)
            if (response != null) {
                val versionCatalog = processResponse(response)
                val url = response.request.url
                if (versionCatalog != null) {
                    send(versionCatalog)
                    debug {
                        Log.i(TAG, "Succeeded to check new version from $url!")
                    }
                } else {
                    Log.w(TAG, "Failed to check new version from $url!")
                }
            }
        }
    }

    private suspend fun sendRequest(source: Request): Response? {
        return try {
            invokeRequest(request = source)
        } catch (e: IOException) {
            Log.w(TAG, "Failed to connect ${source.url}!")
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
     * @param force override [Keys.ignoreUpgradeDate]
     * @return true if new versions available
     */
    @Suppress("KotlinConstantConditions")
    private fun checkUpgradable(versionCatalog: VersionCatalog, force: Boolean): Boolean {

        val currentVersionCode = BuildConfig.VERSION_CODE

        if (versionCatalog.versions.isEmpty()) {
            Log.e(TAG, "VersionCatalog seems corrupted: $versionCatalog")
            return false
        }

        // filter current channel & latest
        val latestVersion = versionCatalog.latest(currentReleaseChannel) ?: versionCatalog.latest
        if (latestVersion == null) {
            Log.e(TAG, "Empty VersionCatalog: $versionCatalog")
            return false
        }

        // check if ignored
        val ignoredDate = Settings(App.instance)[Keys.ignoreUpgradeDate].data
        if (ignoredDate >= latestVersion.date && !force) {
            Log.d(TAG, "ignore this upgrade: ${latestVersion.date}(${dateText(latestVersion.date)})")
            return false
        }


        val latestVersionCode = latestVersion.versionCode
        return when {
            latestVersionCode > currentVersionCode  -> {
                debug { Log.v(TAG, "updatable!") }
                true
            }

            latestVersionCode == currentVersionCode -> {
                debug { Log.v(TAG, "no update, latest version!") }
                false
            }

            latestVersionCode < currentVersionCode  -> {
                debug { Log.w(TAG, "no update, version is newer than latest?") }
                false
            }

            else                                    -> false
        }
    }


    var canAccessGitHub = false
        private set

    private val parser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private const val TAG = "UpdateChecker"

    private const val OWNER = "chr56"
    private const val ORGANIZATION = "Phonograph-Plus"
    private const val ORGANIZATION2 = "PhonographPlus"
    private const val REPO = "Phonograph_Plus"
    private const val BRANCH = "dev"
    private const val FILE = "version_catalog.json"

    const val GITHUB_REPO = "$OWNER/$REPO"

    const val DOMAIN_GITHUB = "github.com"
    const val DOMAIN_TG_LINK = "t.me"

    const val requestUriGitHub = "https://raw.githubusercontent.com/$GITHUB_REPO/$BRANCH/$FILE"
    const val requestUriCodeberg = "https://codeberg.org/$ORGANIZATION2/$REPO/raw/branch/$BRANCH/$FILE"
    const val requestUriBitBucket = "https://bitbucket.org/$ORGANIZATION/$REPO/raw/$BRANCH/$FILE"

    const val requestUriJsdelivr = "https://cdn.jsdelivr.net/gh/$GITHUB_REPO@$BRANCH/$FILE"
    const val requestUriFastGit = "https://endpoint.fastgit.org/https://github.com/$GITHUB_REPO/blob/$BRANCH/$FILE"


    const val CHANNEL_NAME = "Phonograph_Plus"

    const val GITHUB_RELEASE_URL = "$DOMAIN_GITHUB/$GITHUB_REPO/releases"
    const val TG_CHANNEL = "$DOMAIN_TG_LINK/$CHANNEL_NAME"

}