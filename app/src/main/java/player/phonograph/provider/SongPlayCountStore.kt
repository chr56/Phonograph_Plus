/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package player.phonograph.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.min
import player.phonograph.provider.SongPlayCountStore.SongPlayCountColumns.Companion.ID
import player.phonograph.provider.SongPlayCountStore.SongPlayCountColumns.Companion.LAST_UPDATED_WEEK_INDEX
import player.phonograph.provider.SongPlayCountStore.SongPlayCountColumns.Companion.NAME
import player.phonograph.provider.SongPlayCountStore.SongPlayCountColumns.Companion.PLAY_COUNT_SCORE
import player.phonograph.provider.SongPlayCountStore.SongPlayCountColumns.Companion.WEEK_PLAY_COUNT

/**
 * This database tracks the number of play counts for an individual song.  This is used to drive
 * the top played tracks as well as the playlist images
 */
class SongPlayCountStore(context: Context) : SQLiteOpenHelper(context, DatabaseConstants.SONG_PLAY_COUNT_DB, null, VERSION) {

    /** number of weeks since epoch time **/
    private val mNumberOfWeeksSinceEpoch: Int = (System.currentTimeMillis() / ONE_WEEK_IN_MS).toInt()

    /** used to track if we've walked through the db and updated all the rows **/
    private var mDatabaseUpdated: Boolean = false

    override fun onCreate(db: SQLiteDatabase) {
        // create the play count table
        // WARNING: If you change the order of these columns
        // please update getColumnIndexForWeek
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $NAME" +
                "($ID LONG UNIQUE," +
                (0 until NUM_WEEKS).fold("") { x, i -> "$x ${getColumnNameForWeek(i)} INT DEFAULT 0," } +
                "$LAST_UPDATED_WEEK_INDEX INT NOT NULL," +
                "$PLAY_COUNT_SCORE REAL DEFAULT 0;"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $NAME")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // If we ever have downgrade, drop the table to be safe
        db.execSQL("DROP TABLE IF EXISTS $NAME")
        onCreate(db)
    }

    /**
     * Increases the play count of a song by 1
     *
     * @param songId The song id to increase the play count
     */
    fun bumpPlayCount(songId: Long) {
        if (songId == -1L) {
            return
        }
        updateExistingRow(writableDatabase, songId, true)
    }

    /**
     * This creates a new entry that indicates a song has been played once as well as its score
     *
     * @param database a write able database
     * @param songId   the id of the track
     */
    private fun createNewPlayedEntry(database: SQLiteDatabase, songId: Long) {
        // no row exists, create a new one
        database.insert(
            NAME, null,
            ContentValues(3).also { values ->
                values.put(ID, songId)
                values.put(PLAY_COUNT_SCORE, getScoreMultiplierForWeek(0))
                values.put(LAST_UPDATED_WEEK_INDEX, mNumberOfWeeksSinceEpoch)
                values.put(getColumnNameForWeek(0), 1)
            }
        )
    }

    /**
     * This function will take a song entry and update it to the latest week and increase the count
     * for the current week by 1 if necessary
     *
     * @param database  a writeable database
     * @param id        the id of the track to bump
     * @param bumpCount whether to bump the current's week play count by 1 and adjust the score
     */
    private fun updateExistingRow(database: SQLiteDatabase, id: Long, bumpCount: Boolean) {
        val stringId = id.toString()

        // begin the transaction
        database.beginTransaction()

        // get the cursor of this content inside the transaction
        val cursor = database.query(NAME, null, WHERE_ID_EQUALS, arrayOf(stringId), null, null, null)

        // if we have a result
        if (cursor != null && cursor.moveToFirst()) {
            // figure how many weeks since we last updated

            val lastUpdatedWeek = cursor.getInt(cursor.getColumnIndex(LAST_UPDATED_WEEK_INDEX).requireNotNegative())
            val weekDiff = mNumberOfWeeksSinceEpoch - lastUpdatedWeek

            // if it's more than the number of weeks we track, delete it and create a new entry
            if (abs(weekDiff) >= NUM_WEEKS) {
                // this entry needs to be dropped since it is too outdated
                deleteEntry(database, stringId)
                if (bumpCount) {
                    createNewPlayedEntry(database, id)
                }
            } else if (weekDiff != 0) {
                // else, shift the weeks
                val playCounts = IntArray(NUM_WEEKS)
                if (weekDiff > 0) {
                    // time is shifted forwards
                    for (i in 0 until NUM_WEEKS - weekDiff) {
                        playCounts[i + weekDiff] = cursor.getInt(getColumnIndexForWeek(i))
                    }
                } else if (weekDiff < 0) {
                    // time is shifted backwards (by user) - nor typical behavior but we
                    // will still handle it

                    // since weekDiff is -ve, NUM_WEEKS + weekDiff is the real # of weeks we have to
                    // transfer.  Then we transfer the old week i - weekDiff to week i
                    // for example if the user shifted back 2 weeks, ie -2, then for 0 to
                    // NUM_WEEKS + (-2) we set the new week i = old week i - (-2) or i+2
                    for (i in 0 until NUM_WEEKS + weekDiff) {
                        playCounts[i] = cursor.getInt(getColumnIndexForWeek(i - weekDiff))
                    }
                }

                // bump the count
                if (bumpCount) {
                    playCounts[0]++
                }
                val score = calculateScore(playCounts)

                // if the score is non-existent, then delete it
                if (score < .01f) {
                    deleteEntry(database, stringId)
                } else {
                    // create the content values
                    val values = ContentValues(NUM_WEEKS + 2)
                    values.put(LAST_UPDATED_WEEK_INDEX, mNumberOfWeeksSinceEpoch)
                    values.put(PLAY_COUNT_SCORE, score)
                    for (i in 0 until NUM_WEEKS) {
                        values.put(getColumnNameForWeek(i), playCounts[i])
                    }

                    // update the entry
                    database.update(NAME, values, WHERE_ID_EQUALS, arrayOf(stringId))
                }
            } else if (bumpCount) {
                // else no shifting, just update the scores
                // update the entry
                database.update(
                    NAME,
                    ContentValues(2).apply {
                        // increase the score by a single score amount
                        put(
                            PLAY_COUNT_SCORE,
                            cursor.getFloat(cursor.getColumnIndex(PLAY_COUNT_SCORE).requireNotNegative()) +
                                getScoreMultiplierForWeek(0)
                        )
                        put(getColumnNameForWeek(0), cursor.getInt(getColumnIndexForWeek(0)) + 1) // increase the play count by 1
                    },
                    WHERE_ID_EQUALS, arrayOf(stringId)
                )
            }
            cursor.close()
        } else if (bumpCount) {
            // if we have no existing results, create a new one
            createNewPlayedEntry(database, id)
        }
        database.setTransactionSuccessful()
        database.endTransaction()
    }

    fun clear() {
        writableDatabase.delete(NAME, null, null)
    }

    /**
     * Gets a cursor containing the top songs played.  Note this only returns songs that have been
     * played at least once in the past NUM_WEEKS
     *
     * @param numResults number of results to limit by.  If <= 0 it returns all results
     * @return the top tracks
     */
    fun getTopPlayedResults(numResults: Int): Cursor {
        updateResults()
        val database = readableDatabase
        return database.query(
            NAME, arrayOf(ID),
            null, null, null, null, "$PLAY_COUNT_SCORE DESC",
            if (numResults <= 0) null else numResults.toString()
        )
    }

    /**
     * This updates all the results for the getTopPlayedResults so that we can get an
     * accurate list of the top played results
     */
    @Synchronized
    private fun updateResults(force: Boolean = false) {
        if (mDatabaseUpdated && !force) {
            return
        }
        val database = writableDatabase
        database.beginTransaction()
        val oldestWeekWeCareAbout = mNumberOfWeeksSinceEpoch - NUM_WEEKS + 1
        // delete rows we don't care about anymore
        database.delete(
            NAME,
            "$LAST_UPDATED_WEEK_INDEX < $oldestWeekWeCareAbout",
            null
        )

        // get the remaining rows
        val cursor = database.query(
            NAME, arrayOf(ID),
            null, null, null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            // for each row, update it
            do {
                updateExistingRow(database, cursor.getLong(0), false)
            } while (cursor.moveToNext())
            cursor.close()
        }
        mDatabaseUpdated = true
        database.setTransactionSuccessful()
        database.endTransaction()
    }

    fun forceUpdate() {
        updateResults(true)
    }

    /**
     * @param songId The song Id to remove.
     */
    fun removeItem(songId: Long) {
        deleteEntry(writableDatabase, songId.toString())
    }

    /**
     * Deletes the entry
     *
     * @param database database to use
     * @param stringId id to delete
     */
    private fun deleteEntry(database: SQLiteDatabase, stringId: String) {
        database.delete(NAME, WHERE_ID_EQUALS, arrayOf(stringId))
    }

    interface SongPlayCountColumns {
        companion object {
            const val NAME = "song_play_count"
            const val ID = "song_id"
            const val WEEK_PLAY_COUNT = "week"
            const val LAST_UPDATED_WEEK_INDEX = "week_index"
            const val PLAY_COUNT_SCORE = "play_count_score"
        }
    }

    companion object {
        private var sInstance: SongPlayCountStore? = null

        /**
         * @param context The [Context] to use
         * @return A new instance of this class.
         */
        @Synchronized
        fun getInstance(context: Context): SongPlayCountStore {
            if (sInstance == null) {
                sInstance = SongPlayCountStore(context.applicationContext)
            }
            return sInstance!!
        }

        /**
         * Calculates the score of the song given the play counts
         *
         * @param playCounts an array of the # of times a song has been played for each week
         * where playCounts[N] is the # of times it was played N weeks ago
         * @return the score
         */
        private fun calculateScore(playCounts: IntArray?): Float {
            if (playCounts == null) {
                return 0f
            }
            var score = 0f
            for (i in 0 until min(playCounts.size, NUM_WEEKS)) {
                score += playCounts[i] * getScoreMultiplierForWeek(i)
            }
            return score
        }

        /**
         * Gets the column name for each week #
         *
         * @param week number
         * @return the column name
         */
        private fun getColumnNameForWeek(week: Int): String = WEEK_PLAY_COUNT + week.toString()

        /** interpolator curve applied for measuring the curve */
        private val sInterpolator: Interpolator = AccelerateInterpolator(1.5f)

        /**
         * Gets the score multiplier for each week
         *
         * @param week number
         * @return the multiplier to apply
         */
        private fun getScoreMultiplierForWeek(week: Int): Float =
            sInterpolator.getInterpolation(1 - week / NUM_WEEKS.toFloat()) * INTERPOLATOR_HEIGHT + INTERPOLATOR_BASE

        /**
         * For some performance gain, return a static value for the column index for a week
         * WARNING: This function assumes you have selected all columns for it to work
         *
         * @param week number
         * @return column index of that week
         */
        private fun getColumnIndexForWeek(week: Int): Int = 1 + week // ID, followed by the weeks columns

        private const val VERSION = 3

        /** how many weeks worth of playback to track */
        private const val NUM_WEEKS = 52

        /** how high to multiply the interpolation curve */
        private const val INTERPOLATOR_HEIGHT = 50

        // how high the base value is. The ratio of the Height to Base is what really matters
        private const val INTERPOLATOR_BASE = 25
        private const val ONE_WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7
        private const val WHERE_ID_EQUALS = "$ID=?"
    }
}
