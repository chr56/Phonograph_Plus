/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.metadata

import androidx.annotation.Keep

@Keep
enum class MusicTagFormat(val id: String) {

    ID3v1("ID3v1Tag"),
    ID3v11("ID3v11Tag"),
    ID3v24("ID3v24Tag"),
    ID3v22("ID3v22Tag"),
    ID3v23("ID3v23Tag"),
    Mp4("Mp4Tag"),
    VorbisComment("VorbisCommentTag"),
    Flac("FlacTag"),
    Aiff("AiffTag"),
    Asf("AsfTag"),
    Real("RealTag"),
    Wav("WavTag"),
    WavInfo("WavInfoTag"),
    Unknown("N/A"),
    ;
}
