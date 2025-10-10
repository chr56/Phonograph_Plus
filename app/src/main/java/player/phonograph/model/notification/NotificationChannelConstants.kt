/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.notification

import androidx.annotation.StringDef

const val NOTIFICATION_CHANNEL_ID_DEFAULT = "default_notification"
const val NOTIFICATION_CHANNEL_ID_ERROR = "error_notification"
const val NOTIFICATION_CHANNEL_ID_UPGRADE = "upgrade_notification"
const val NOTIFICATION_CHANNEL_ID_BACKGROUND = "background_notification"

val NOTIFICATION_CHANNEL_IDS
    get() = setOf(
        NOTIFICATION_CHANNEL_ID_DEFAULT,
        NOTIFICATION_CHANNEL_ID_ERROR,
        NOTIFICATION_CHANNEL_ID_UPGRADE,
        NOTIFICATION_CHANNEL_ID_BACKGROUND,
    )

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    NOTIFICATION_CHANNEL_ID_DEFAULT,
    NOTIFICATION_CHANNEL_ID_ERROR,
    NOTIFICATION_CHANNEL_ID_UPGRADE,
    NOTIFICATION_CHANNEL_ID_BACKGROUND
)
annotation class ChannelID

