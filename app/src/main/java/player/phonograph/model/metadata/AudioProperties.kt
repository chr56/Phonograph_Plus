/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.metadata

import player.phonograph.R

data class AudioProperties(
    val audioFormat: String,
    val trackLength: Long,
    val bitRate: Long,
    val samplingRate: Long,
) : Metadata {

    override fun get(key: Metadata.Key): Metadata.Field? =
        when (key) {
            is Keys.AudioFormat  -> Metadata.TextualField(audioFormat)
            is Keys.TrackLength  -> Metadata.NumericField(trackLength, NOTATION_DURATION)
            is Keys.BitRate      -> Metadata.NumericField(bitRate, NOTATION_BIT_RATE)
            is Keys.SamplingRate -> Metadata.NumericField(samplingRate, NOTATION_SAMPLING)
            else                 -> null
        }

    override fun contains(key: Metadata.Key): Boolean = key is AudioPropertiesKey

    override val fields: List<Metadata.Entry>
        get() = listOf(
            Metadata.PlainEntry(Keys.AudioFormat, Metadata.TextualField(audioFormat)),
            Metadata.PlainEntry(Keys.TrackLength, Metadata.NumericField(trackLength, NOTATION_DURATION)),
            Metadata.PlainEntry(Keys.BitRate, Metadata.NumericField(bitRate, NOTATION_BIT_RATE)),
            Metadata.PlainEntry(Keys.SamplingRate, Metadata.NumericField(samplingRate, NOTATION_SAMPLING)),
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