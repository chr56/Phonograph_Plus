/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.notification

import androidx.annotation.Keep
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Parcelize
@Serializable
data class NotificationActionsConfig(
    @SerialName("actions") val actions: List<Item>,
    @SerialName("version") val version: Int = VERSION,
) : Parcelable {

    constructor(vararg sources: Item) : this(sources.asList(), VERSION)

    @Keep
    @Parcelize
    @Serializable
    data class Item(
        @SerialName("key") @param:NotificationActionName val key: String,
        @SerialName("compat") var displayInCompat: Boolean = false,
    ) : Parcelable {

        @Contextual
        @IgnoredOnParcel
        val notificationAction: NotificationAction = NotificationAction.from(key)
    }

    companion object {
        val DEFAULT: NotificationActionsConfig
            get() = NotificationActionsConfig(
                Item(ACTION_KEY_REPEAT),
                Item(ACTION_KEY_PREV, displayInCompat = true),
                Item(ACTION_KEY_PLAY_PAUSE, displayInCompat = true),
                Item(ACTION_KEY_NEXT, displayInCompat = true),
                Item(ACTION_KEY_SHUFFLE),
            )
        const val VERSION = 1
    }
}