/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package player.phonograph.helper

import android.provider.MediaStore

/**
 * Holds all of the sort orders for each list type.
 *
 * @author Andrew Neal (andrewdneal@gmail.com) , chr_56 (modified)
 */
object SortOrder {
    /**
     * Artist sort order entries.
     */
    interface ArtistSortOrder {
        companion object {
            /* Artist sort order A-Z */
            const val ARTIST_A_Z = MediaStore.Audio.Media.ARTIST

            /* Artist sort order Z-A */
            const val ARTIST_Z_A = "$ARTIST_A_Z DESC"
// Crashes!!
//            /* Artist sort order number of songs */
//            const val ARTIST_NUMBER_OF_SONGS = MediaStore.Audio.Artists.NUMBER_OF_TRACKS + " DESC"
//
//            /* Artist sort order number of songs (less to more)*/
//            const val ARTIST_NUMBER_OF_SONGS_REVERT = MediaStore.Audio.Artists.NUMBER_OF_TRACKS
//
//            /* Artist sort order number of albums */
//            const val ARTIST_NUMBER_OF_ALBUMS = MediaStore.Audio.Artists.NUMBER_OF_ALBUMS + " DESC"
//
//            /* Artist sort order number of albums (less to more)*/
//            const val ARTIST_NUMBER_OF_ALBUMS_REVERT = MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
        }
    }

    /**
     * Album sort order entries.
     */
    interface AlbumSortOrder {
        companion object {
            /* Album sort order A-Z */
            const val ALBUM_A_Z = MediaStore.Audio.Media.ALBUM

            /* Album sort order Z-A */
            const val ALBUM_Z_A = "$ALBUM_A_Z DESC"

// CRASHES!!
//
//            /* Album sort order songs (less to more)*/
//            const val ALBUM_NUMBER_OF_SONGS = MediaStore.Audio.Albums.NUMBER_OF_SONGS
//
//            /* Album sort order songs (more to less)*/
//            const val ALBUM_NUMBER_OF_SONGS_REVERT = MediaStore.Audio.Albums.NUMBER_OF_SONGS + " DESC"

            /* Album sort order artist */
            const val ALBUM_ARTIST = MediaStore.Audio.Media.ARTIST + ", " + MediaStore.Audio.Media.ALBUM

            /* Album sort order artist */
            const val ALBUM_ARTIST_REVERT = MediaStore.Audio.Media.ARTIST + " DESC" + ", " + MediaStore.Audio.Media.ALBUM + " DESC"

            /* Album sort order year (old to new)*/
            const val ALBUM_YEAR = MediaStore.Audio.Media.YEAR

            /* Album sort order year (new to old)*/
            const val ALBUM_YEAR_REVERT = MediaStore.Audio.Media.YEAR + " DESC"
        }
    }

    /**
     * Song sort order entries.
     */
    interface SongSortOrder {
        companion object {
            /* Song sort order A-Z */
            const val SONG_A_Z = MediaStore.Audio.Media.TITLE

            /* Song sort order Z-A */
            const val SONG_Z_A = "$SONG_A_Z DESC"

            /* Song sort order artist A-Z */
            const val SONG_ARTIST = MediaStore.Audio.Media.ARTIST

            /* Song sort order artist Z-A*/
            const val SONG_ARTIST_REVERT = "$SONG_ARTIST DESC"

            /* Song sort order album A-Z*/
            const val SONG_ALBUM = MediaStore.Audio.Media.ALBUM

            /* Song sort order album Z-A*/
            const val SONG_ALBUM_REVERT = "$SONG_ALBUM DESC"

            /* Song sort order year (new to old)*/
            const val SONG_YEAR = MediaStore.Audio.Media.YEAR

            /* Song sort order year (old to new)*/
            const val SONG_YEAR_REVERT = "$SONG_YEAR DESC"

            /* Song sort order duration (short to long)*/
            const val SONG_DURATION = MediaStore.Audio.Media.DURATION

            /* Song sort order duration (long to old)*/
            const val SONG_DURATION_REVERT = "$SONG_DURATION DESC"

            /* Song sort order add date (new to old)*/
            const val SONG_DATE = MediaStore.Audio.Media.DATE_ADDED

            /* Song sort order add date (old to new)*/
            const val SONG_DATE_REVERT = "$SONG_DATE DESC"

            /* Song sort order modified date (new to old) */
            const val SONG_DATE_MODIFIED = MediaStore.Audio.Media.DATE_MODIFIED

            /* Song sort order modified date (old to new)*/
            const val SONG_DATE_MODIFIED_REVERT = "$SONG_DATE_MODIFIED DESC"
        }
    }

    /**
     * Album song sort order entries.
     */
    interface AlbumSongSortOrder {
        companion object {
            /* Album song sort order A-Z */
            const val SONG_A_Z = MediaStore.Audio.Media.TITLE

            /* Album song sort order Z-A */
            const val SONG_Z_A = "$SONG_A_Z DESC"

            /* Album song sort order track list */
            const val SONG_TRACK_LIST = MediaStore.Audio.Media.TRACK + ", " + MediaStore.Audio.Media.TITLE

            /* Album song sort order duration */
            const val SONG_DURATION = SongSortOrder.SONG_DURATION

            /* Song sort order add date */
            const val SONG_DATE = MediaStore.Audio.Media.DATE_ADDED + " DESC"

            /* Song sort order add date (old to new)*/
            const val SONG_DATE_REVERT = MediaStore.Audio.Media.DATE_ADDED

            /* Song sort order modified date */
            const val SONG_DATE_MODIFIED = MediaStore.Audio.Media.DATE_MODIFIED + " DESC"

            /* Song sort order modified date (old to new)*/
            const val SONG_DATE_MODIFIED_REVERT = MediaStore.Audio.Media.DATE_MODIFIED
        }
    }

    /**
     * Artist song sort order entries.
     */
    interface ArtistSongSortOrder {
        companion object {
            /* Artist song sort order A-Z */
            const val SONG_A_Z = MediaStore.Audio.Media.TITLE

            /* Artist song sort order Z-A */
            const val SONG_Z_A = "$SONG_A_Z DESC"

            /* Artist song sort order album */
            const val SONG_ALBUM = MediaStore.Audio.Media.ALBUM

            /* Artist song sort order year */
            const val SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC"

            /* Song sort order year (old to new)*/
            const val SONG_YEAR_REVERT = MediaStore.Audio.Media.YEAR

            /* Artist song sort order duration */
            const val SONG_DURATION = MediaStore.Audio.Media.DURATION + " DESC"

            /* Artist song sort order date */
            const val SONG_DATE = MediaStore.Audio.Media.DATE_ADDED + " DESC"

            /* Song sort order add date (old to new)*/
            const val SONG_DATE_REVERT = MediaStore.Audio.Media.DATE_ADDED

            /* Song sort order modified date */
            const val SONG_DATE_MODIFIED = MediaStore.Audio.Media.DATE_MODIFIED + " DESC"

            /* Song sort order modified date (old to new)*/
            const val SONG_DATE_MODIFIED_REVERT = MediaStore.Audio.Media.DATE_MODIFIED
        }
    }

    /**
     * Artist album sort order entries.
     */
    interface ArtistAlbumSortOrder {
        companion object {
            /* Artist album sort order A-Z */
            const val ALBUM_A_Z = MediaStore.Audio.Media.ALBUM

            /* Artist album sort order Z-A */
            const val ALBUM_Z_A = "$ALBUM_A_Z DESC"

            /* Artist album sort order year */
            const val ALBUM_YEAR = (
                MediaStore.Audio.Media.YEAR +
                    " DESC"
                )

            /* Artist album sort order year */
            const val ALBUM_YEAR_ASC = (
                MediaStore.Audio.Media.YEAR +
                    " ASC"
                )
        }
    }

    /**
     * Genre sort order entries.
     */
    interface GenreSortOrder {
        companion object {
            /* Genre sort order A-Z */
            const val GENRE_A_Z = MediaStore.Audio.Genres.NAME

            /* Genre sort order Z-A */
            const val ALBUM_Z_A = "$GENRE_A_Z DESC"
        }
    }
}
