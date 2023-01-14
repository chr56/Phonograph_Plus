/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util

import StatusBarLyric.API.StatusBarLyric
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

object StatusBarLyricUtil {
    // Actually, ServiceName is (music) service name, so we have no suffix (.plus.BUILD_TYPE)
    private const val musicServiceName = PACKAGE_NAME
    private const val useSystemMusicActive = false
    private val icon: Drawable? = AppCompatResources.getDrawable(App.instance, R.drawable.ic_notification)

    fun updateLyric(lyric: String) {
        if (Setting.instance.broadcastSynchronizedLyrics) {
            if (lyricsService.hasEnable()) {
                lyricsService.updateLyric(lyric)
            } else {
                legacyUpdateLyrics(lyric)
            }
        }
    }

    fun stopLyric() {
        if (Setting.instance.broadcastSynchronizedLyrics) {
            if (lyricsService.hasEnable()) {
                lyricsService.stopLyric()
            } else {
                legacyStopLyrics()
            }
        }
    }
    /**
     *  StatusBar Lyrics API
     */
    private val lyricsService: StatusBarLyric by lazy {
        StatusBarLyric(App.instance, icon, musicServiceName, useSystemMusicActive)
    }

    private fun legacyUpdateLyrics(lyric: String) {
        Log.d(TAG, "use fallback api: $lyric")
        if (lyric.isNotEmpty()) {
            App.instance.sendBroadcast(
                Intent().setAction("Lyric_Server")
                    .putExtra("Lyric_Type", "app")
                    .putExtra("Lyric_Data", lyric)
                    .putExtra("Lyric_PackName", musicServiceName)
                    .putExtra("Lyric_UseSystemMusicActive", useSystemMusicActive)
                    .putExtra("Lyric_Icon", (icon as BitmapDrawable?)?.toBase64())
            )
        }
    }

    private fun legacyStopLyrics() {
        App.instance.sendBroadcast(
            Intent().setAction("Lyric_Server").putExtra("Lyric_Type", "app_stop")
        )
    }

    private fun BitmapDrawable.toBase64(): String {
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\n", "")
    }

    private const val TAG = "statusbar_lyric"
}