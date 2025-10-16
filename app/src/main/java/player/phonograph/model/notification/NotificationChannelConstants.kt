/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.notification

import androidx.annotation.StringDef

const val NOTIFICATION_CHANNEL_ID_DEFAULT = "default_notification"
const val NOTIFICATION_CHANNEL_ID_ERROR_REPORT = "error_notification"
const val NOTIFICATION_CHANNEL_ID_APP_UPDATE = "upgrade_notification"
const val NOTIFICATION_CHANNEL_ID_BACKGROUND_TASKS = "background_notification"
const val NOTIFICATION_CHANNEL_ID_DATABASE_SYNC = "database_notification"

val NOTIFICATION_CHANNEL_IDS
    get() = setOf(
        NOTIFICATION_CHANNEL_ID_DEFAULT,
        NOTIFICATION_CHANNEL_ID_ERROR_REPORT,
        NOTIFICATION_CHANNEL_ID_APP_UPDATE,
        NOTIFICATION_CHANNEL_ID_BACKGROUND_TASKS,
        NOTIFICATION_CHANNEL_ID_DATABASE_SYNC,
    )

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    NOTIFICATION_CHANNEL_ID_DEFAULT,
    NOTIFICATION_CHANNEL_ID_ERROR_REPORT,
    NOTIFICATION_CHANNEL_ID_APP_UPDATE,
    NOTIFICATION_CHANNEL_ID_BACKGROUND_TASKS,
    NOTIFICATION_CHANNEL_ID_DATABASE_SYNC,
)
annotation class ChannelID

