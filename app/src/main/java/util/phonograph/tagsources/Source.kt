/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources

sealed class Source(val name: String) {
    object MusicBrainz : Source("MusicBrainz")
    object LastFm : Source("last.fm")
}