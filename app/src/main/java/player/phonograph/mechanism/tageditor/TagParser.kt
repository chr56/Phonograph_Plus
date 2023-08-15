/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tageditor

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v22Frames
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Frames
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Frames
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import player.phonograph.util.reportError


fun readAllTags(audioFile: AudioFile): Map<String, String> {
    val items: Map<String, String> =
        when (val tag = audioFile.tag) {
            is AbstractID3v2Tag -> readID3v2Tags(tag)
            else                -> emptyMap()
        }
    return items
}

fun readID3v2Tags(tag: AbstractID3v2Tag): Map<String, String> {
    return tag.frameMap
        .mapKeys { (key, frame) ->
            val frames = when (tag) {
                is ID3v24Tag -> ID3v24Frames.getInstanceOf()
                is ID3v23Tag -> ID3v23Frames.getInstanceOf()
                is ID3v22Tag -> ID3v22Frames.getInstanceOf()
                else         -> null
            }
            if (frames != null) {
                val description = frames.idToValueMap.getOrDefault(key, ERR_PARSE_KEY)
                "[$key]$description"
            } else {
                key
            }
        }
        .mapValues { (key, data) ->
            when (data) {
                is TagField -> {
                    parseTagField(data) {
                        if (data is AbstractID3v2Frame) {
                            parseID3v2Frame(data)
                        } else {
                            data.rawContent.toString()
                        }
                    }
                }

                is List<*>  -> {
                    data.map { item ->
                        if (item is TagField)
                            parseTagField(item) {
                                if (it is AbstractID3v2Frame) {
                                    parseID3v2Frame(it)
                                } else {
                                    it.rawContent.toString()
                                }
                            }
                        else
                            item.toString()
                    }.joinToString(separator = "\n") { it }
                }

                else        -> data.toString()
            }
        }
}

private fun parseID3v2Frame(frame: AbstractID3v2Frame): String {
    return try {
        when (val frameBody = frame.body) {
            is FrameBodyTXXX -> "${frameBody.description}:\n\t${frameBody.userFriendlyValue}"
            else             -> frameBody.userFriendlyValue
        }
    } catch (e: Exception) {
        reportError(e, "readID3v2Tags", ERR_PARSE_FIELD)
        ERR_PARSE_FIELD
    }
}

private inline fun parseTagField(
    frame: TagField,
    block: (frame: TagField) -> String,
): String =
    when {
        frame.isBinary -> BINARY
        frame.isEmpty  -> EMPTY
        else           -> block(frame)
    }

private const val BINARY = "<Binary Data>"
private const val EMPTY = "<Empty>"

private const val ERR_PARSE_FIELD = "<Err: failed to read field>"
private const val ERR_PARSE_KEY = "<Err: failed to process key>"