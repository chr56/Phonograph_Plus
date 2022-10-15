/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.Spanned
import androidx.core.app.NotificationCompat
import player.phonograph.R
import player.phonograph.UPGRADABLE
import player.phonograph.VERSION_INFO
import player.phonograph.model.version.Version
import player.phonograph.model.version.VersionCatalog
import player.phonograph.ui.activities.MainActivity
import java.util.*

class UpgradeNotificationImpl2(context: Context) : AbsNotificationImpl() {

    override val channelId: String = NOTIFICATION_CHANNEL_ID_UPGRADABLE
    override val channelName: CharSequence = context.getString(R.string.upgrade_notification_name)
    override val importance: Int = NotificationManager.IMPORTANCE_HIGH

    fun sendUpgradeNotification(context: Context, versionCatalog: VersionCatalog, channel: String) {
        execute(context) {
            val version = versionCatalog.versions.filter { it.channel == channel }.maxByOrNull { it.versionCode } ?: return
            val action = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                this.putExtra(UPGRADABLE, true)
                this.putExtra(VERSION_INFO, versionCatalog)
            }
            val clickIntent: PendingIntent =
                PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val title = "${context.getString(R.string.new_version_code)} -- ${version.versionName}"
            val note = version.releaseNote.parsed(context.resources)
            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_UPGRADABLE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(context.getText(R.string.new_version))
                    .setContentText("$title\n$note")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle(title)
                            .setSummaryText(note)
                    )
                    .setContentIntent(clickIntent)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .build()
            notificationManager.notify(NOTIFICATION_ID_UPGRADABLE, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID_UPGRADABLE = "upgrade_notification"
        private const val NOTIFICATION_ID_UPGRADABLE = 8747233

        private fun parseReleaseNote(context: Context, releaseNote: Version.ReleaseNote): Spanned? {
            val lang = context.resources.configuration.locales.get(0)
            val zhs = Locale.SIMPLIFIED_CHINESE
            val source = if (lang.equals(zhs)) {
                releaseNote.zh_cn
            } else {
                releaseNote.en
            }
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        }
    }

}
