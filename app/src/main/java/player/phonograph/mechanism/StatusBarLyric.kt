/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import player.phonograph.App
import player.phonograph.PACKAGE_NAME
import player.phonograph.R
import player.phonograph.settings.Setting
import androidx.appcompat.content.res.AppCompatResources
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import StatusBarLyric.API.StatusBarLyric as StatusBarLyricAPI

object StatusBarLyric {
    // Actually, ServiceName is (music) service name, so we have no suffix (.plus.BUILD_TYPE)
    private const val musicServiceName = PACKAGE_NAME
    private const val useSystemMusicActive = false
    private val icon: Drawable? = AppCompatResources.getDrawable(App.instance, R.drawable.ic_notification)

    fun updateLyric(lyric: String) {
        if (Setting.instance.broadcastSynchronizedLyrics) {
            lyricsService.updateLyric(lyric)
        }
    }

    fun stopLyric() {
        if (Setting.instance.broadcastSynchronizedLyrics) {
            lyricsService.stopLyric()
        }
    }
    /**
     *  StatusBar Lyrics API
     */
    private val lyricsService: StatusBarLyricAPI by lazy {
        StatusBarLyricAPI(App.instance, icon, musicServiceName, useSystemMusicActive)
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