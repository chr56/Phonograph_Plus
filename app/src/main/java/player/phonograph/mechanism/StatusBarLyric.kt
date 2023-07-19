/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import cn.lyric.getter.api.tools.EventTools
import player.phonograph.App
import player.phonograph.PACKAGE_NAME
import player.phonograph.R
import player.phonograph.service.MusicService
import player.phonograph.settings.Setting
import androidx.appcompat.content.res.AppCompatResources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream
import StatusBarLyric.API.StatusBarLyric as StatusBarLyricAPI

object StatusBarLyric {
    // Actually, ServiceName is (music) service name, so we have no suffix (.plus.BUILD_TYPE)
    private const val musicServicePackageName = PACKAGE_NAME
    private val musicServiceName = MusicService::class.java.canonicalName!!
    private val icon: Drawable? = AppCompatResources.getDrawable(App.instance, R.drawable.ic_notification)
    private val iconBase64: String? = (icon as BitmapDrawable?)?.toBase64()

    fun updateLyric(lyric: String) {
        if (Setting.instance.broadcastSynchronizedLyrics) {
            if (Setting.instance.useLegacyStatusBarLyricsApi) {
                lyricsService.updateLyric(lyric)
            } else {
                EventTools.sendLyric(
                    context = App.instance,
                    lyric = lyric,
                    customIcon = iconBase64 != null,
                    base64Icon = iconBase64!!,
                    useOwnMusicController = true,
                    serviceName = musicServiceName,
                    packageName = musicServicePackageName
                )
            }
        }
    }

    fun stopLyric() {
        if (Setting.instance.broadcastSynchronizedLyrics) {
            if (Setting.instance.useLegacyStatusBarLyricsApi) {
                lyricsService.stopLyric()
            } else {
                EventTools.stopLyric(App.instance)
            }
        }
    }
    /**
     *  StatusBar Lyrics API
     */
    private val lyricsService: StatusBarLyricAPI by lazy {
        StatusBarLyricAPI(App.instance, icon, musicServicePackageName, false)
    }

    private fun BitmapDrawable.toBase64(): String {
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\n", "")
    }

    private const val TAG = "StatusBarAPI"
}