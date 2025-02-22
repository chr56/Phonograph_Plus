/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.metadata

import player.phonograph.R

data class AudioProperties(
    val audioFormat: String,
    val trackLength: Long,
    val bitRate: String,
    val samplingRate: String,
) : Metadata {

    override fun get(key: Metadata.Key): Metadata.Field? =
        when (key) {
            is Keys.AudioFormat  -> Metadata.PlainStringField(audioFormat)
            is Keys.TrackLength  -> Metadata.PlainNumberField(trackLength)
            is Keys.BitRate      -> Metadata.PlainStringField(bitRate)
            is Keys.SamplingRate -> Metadata.PlainStringField(samplingRate)
            else                 -> null
        }

    override fun contains(key: Metadata.Key): Boolean = key is AudioPropertiesKey

    override val fields: List<Metadata.Entry>
        get() = listOf(
            Metadata.PlainEntry(Keys.AudioFormat, Metadata.PlainStringField(audioFormat)),
            Metadata.PlainEntry(Keys.TrackLength, Metadata.PlainNumberField(trackLength)),
            Metadata.PlainEntry(Keys.BitRate, Metadata.PlainStringField(bitRate)),
            Metadata.PlainEntry(Keys.SamplingRate, Metadata.PlainStringField(samplingRate)),
        )

    sealed interface AudioPropertiesKey : Metadata.Key

    object Keys {

        data object AudioFormat : AudioPropertiesKey {
            override val res: Int = R.string.label_file_format
        }

        data object TrackLength : AudioPropertiesKey {
            override val res: Int = R.string.label_track_length
        }

        data object BitRate : AudioPropertiesKey {
            override val res: Int = R.string.label_bit_rate
        }

        data object SamplingRate : AudioPropertiesKey {
            override val res: Int = R.string.label_sampling_rate
        }

    }
}