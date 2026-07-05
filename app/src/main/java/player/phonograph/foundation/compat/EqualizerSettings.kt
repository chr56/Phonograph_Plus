/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.compat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.audiofx.AudioEffect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU


fun checkEqualizer(packageManager: PackageManager): ResolveInfo? {
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    val resolveInfo = try {
        if (SDK_INT > TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, 0)
        }
    } catch (_: Exception) {
        null
    }
    return resolveInfo
}

fun openEqualizer(activity: Activity, sessionId: Int = 0): Boolean =
    try {
        activity.startActivityForResult(
            Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                if (sessionId > 0) {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                }
            }, 0
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
