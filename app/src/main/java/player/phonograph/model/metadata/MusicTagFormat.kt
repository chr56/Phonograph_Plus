/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.metadata

import androidx.annotation.Keep

@Keep
enum class MusicTagFormat(val id: String) {

    Mp4("Mpeg-4 Metadata"),
    ID3v24("ID3 v2.4"),
    ID3v23("ID3 v2.3"),
    ID3v22("ID3 v2.2"),
    ID3v11("ID3 v1.1"),
    ID3v1("ID3 v1"),
    Flac("Vorbis Comment for FLAC"),
    Aiff("ID3 for Aiff"),
    Asf("ASF for WMA"),
    Real("Real Metadata"),
    Wav("ID3 for WAV"),
    WavInfo("WAV List Info"),
    VorbisComment("Vorbis Comment"),
    Unknown("N/A"),
    ;
}
