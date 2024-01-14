/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import cn.lyric.getter.api.API
import cn.lyric.getter.api.data.ExtraData
import player.phonograph.App
import player.phonograph.PACKAGE_NAME
import player.phonograph.R
import player.phonograph.service.MusicService
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
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
    private val icon: Drawable? get() = AppCompatResources.getDrawable(App.instance, R.drawable.ic_notification)
    private val iconBase64: String? = icon?.toBase64()


    private val extraData: ExtraData = ExtraData(
        iconBase64 != null,
        iconBase64 ?: "",
        true,
        musicServiceName,
        0
    )
    private val API: API = API()

    fun updateLyric(lyric: String) {
        val setting = Setting(App.instance)
        if (setting[Keys.broadcastSynchronizedLyrics].data) {
            if (setting[Keys.useLegacyStatusBarLyricsApi].data) {
                lyricsService.updateLyric(lyric)
            } else {
                API.sendLyric(lyric = lyric, extra = extraData)
            }
        }
    }

    fun stopLyric() {
        val setting = Setting(App.instance)
        if (setting[Keys.broadcastSynchronizedLyrics].data) {
            if (setting[Keys.useLegacyStatusBarLyricsApi].data) {
                lyricsService.stopLyric()
            } else {
                API.clearLyric()
            }
        }
    }
    /**
     *  StatusBar Lyrics API
     */
    private val lyricsService: StatusBarLyricAPI by lazy {
        StatusBarLyricAPI(App.instance, icon, musicServicePackageName, false)
    }

    private fun Drawable.toBase64(): String {
        val bytes = ByteArrayOutputStream().use { out ->
            val bitmap = when (this) {
                is BitmapDrawable -> bitmap
                else          -> this.toBitmap()
            }
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\n", "")
    }

    private const val TAG = "StatusBarAPI"
}