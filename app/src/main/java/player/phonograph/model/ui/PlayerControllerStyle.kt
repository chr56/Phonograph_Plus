/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.ui

import androidx.annotation.IntDef
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PlayerControllerStyle(
    @SerialName("style") @param:ControllerStyle @field:ControllerStyle val style: Int,
    @SerialName("buttons") val buttons: Map<@ButtonPosition Int, @FunctionType Int>,
) : Parcelable {

    companion object {

        const val STYLE_FLAT = 1
        const val STYLE_CLASSIC = 2


        @IntDef(STYLE_FLAT, STYLE_CLASSIC)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ControllerStyle


        /**
         * Buttons closed to center
         */
        const val BUTTONS_PRIMARY = 1

        /**
         * Buttons in the middle of the rest
         */
        const val BUTTONS_SECONDARY = 2

        /**
         * Buttons marginal
         */
        const val BUTTONS_TERTIARY = 3

        @IntDef(BUTTONS_PRIMARY, BUTTONS_SECONDARY, BUTTONS_TERTIARY)
        @Retention(AnnotationRetention.SOURCE)
        @Target(
            AnnotationTarget.TYPE,
            AnnotationTarget.FIELD,
            AnnotationTarget.PROPERTY,
            AnnotationTarget.VALUE_PARAMETER,
        )
        annotation class ButtonPosition


        /**
         * Previous Song & Next Song
         */
        const val FUNCTION_NONE = 0
        /**
         * Previous Song & Next Song
         */
        const val FUNCTION_SWITCH = 8

        /**
         * Rewind by Seconds & Forward by Seconds
         */
        const val FUNCTION_SEEK = 16

        /**
         * Repeat Mode & Shuffle Mode
         * (Normally)
         */
        const val FUNCTION_QUEUE_MODE_N = 64

        /**
         * Shuffle Mode & Repeat Mode
         * (Alternatively)
         */
        const val FUNCTION_QUEUE_MODE_A = 128

        @IntDef(FUNCTION_NONE, FUNCTION_SWITCH, FUNCTION_SEEK, FUNCTION_QUEUE_MODE_N, FUNCTION_QUEUE_MODE_A)
        @Retention(AnnotationRetention.SOURCE)
        @Target(
            AnnotationTarget.TYPE,
            AnnotationTarget.FIELD,
            AnnotationTarget.PROPERTY,
            AnnotationTarget.VALUE_PARAMETER,
        )
        annotation class FunctionType

        val DEFAULT_BUTTONS: Map<@ButtonPosition Int, @FunctionType Int>
            get() = mapOf(
                BUTTONS_PRIMARY to FUNCTION_SEEK,
                BUTTONS_SECONDARY to FUNCTION_NONE,
                BUTTONS_TERTIARY to FUNCTION_SWITCH,
            )

        val DEFAULT get() = PlayerControllerStyle(STYLE_CLASSIC, DEFAULT_BUTTONS)

    }
}