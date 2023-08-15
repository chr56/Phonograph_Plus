/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tageditor

import org.jaudiotagger.audio.real.RealTag
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.aiff.AiffTag
import org.jaudiotagger.tag.asf.AsfTag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.ID3v11Tag
import org.jaudiotagger.tag.id3.ID3v1Tag
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.wav.WavInfoTag
import org.jaudiotagger.tag.wav.WavTag
import androidx.annotation.Keep

@Keep
enum class TagFormat(val clazz: Class<out Tag>, val id: String) {

    ID3v1(ID3v1Tag::class.java, "ID3v1Tag"),
    ID3v11(ID3v11Tag::class.java, "ID3v11Tag"),
    ID3v24(ID3v24Tag::class.java, "ID3v24Tag"),
    ID3v22(ID3v22Tag::class.java, "ID3v22Tag"),
    ID3v23(ID3v23Tag::class.java, "ID3v23Tag"),
    Mp4(Mp4Tag::class.java, "Mp4Tag"),
    VorbisComment(VorbisCommentTag::class.java, "VorbisCommentTag"),
    Flac(FlacTag::class.java, "FlacTag"),
    Aiff(AiffTag::class.java, "AiffTag"),
    Asf(AsfTag::class.java, "AsfTag"),
    Real(RealTag::class.java, "RealTag"),
    Wav(WavTag::class.java, "WavTag"),
    WavInfo(WavInfoTag::class.java, "WavInfoTag"),
    Unknown(Tag::class.java, "")
    ;

    companion object {
        fun of(type: Class<out Tag>): TagFormat =
            values().firstOrNull { it.clazz == type } ?: Unknown

        fun of(tag: Tag): TagFormat = of(tag.javaClass)
    }
}