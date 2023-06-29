/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.provider

import player.phonograph.R
import player.phonograph.notification.BackgroundNotification
import player.phonograph.notification.ErrorNotification
import player.phonograph.repo.provider.SongPlayCountStore.SongPlayCountColumns.Companion.ID
import player.phonograph.repo.provider.SongPlayCountStore.SongPlayCountColumns.Companion.LAST_UPDATED_WEEK_INDEX
import player.phonograph.repo.provider.SongPlayCountStore.SongPlayCountColumns.Companion.NAME
import player.phonograph.repo.provider.SongPlayCountStore.SongPlayCountColumns.Companion.PLAY_COUNT_SCORE
import player.phonograph.repo.provider.SongPlayCountStore.SongPlayCountColumns.Companion.WEEK
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.abs
import kotlin.math.min

/**
 * This database tracks the number of play counts for an individual song.  This is used to drive
 * the top played tracks as well as the playlist images
 */
class SongPlayCountStore(context: Context) : SQLiteOpenHelper(context,
    DatabaseConstants.SONG_PLAY_COUNT_DB, null, VERSION
) {

    /** number of weeks since epoch time **/
    private val mCurrentWeekNumber: Int get() = (System.currentTimeMillis() / ONE_WEEK_IN_MS).toInt()

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
                "$PLAY_COUNT_SCORE REAL DEFAULT 0);"
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
                values.put(LAST_UPDATED_WEEK_INDEX, mCurrentWeekNumber)
                values.put(getColumnNameForWeek(0), 1)
            }
        )
    }

    /**
     * This update existing entry
     * @param database  a write able database
     * @param cursor    cursor that CURRENT point to the entry you want to change
     * @param songId    the id of the track to bump
     * @param bumpCount whether to bump the current's week play count by 1 and adjust the score
     * @param reCalculate re-calculate scores
     * */
    private fun updateExistedPlayedEntry(
        database: SQLiteDatabase,
        cursor: Cursor,
        songId: Long,
        bumpCount: Boolean,
        reCalculate: Boolean = false
    ) {
        // figure how many weeks since we last updated
        val lastUpdatedWeek = cursor.getInt(cursor.getColumnIndex(LAST_UPDATED_WEEK_INDEX).requireNotNegative())
        val weekDiff = mCurrentWeekNumber - lastUpdatedWeek

        when {
            // remove outdated (beyond NUM_WEEKS)
            abs(weekDiff) >= NUM_WEEKS -> {
                // delete it and create a new entry
                deleteEntry(database, songId.toString())
                if (bumpCount) {
                    createNewPlayedEntry(database, songId)
                }
            }
            // shift the weeks if week changes
            // or force refresh score required
            weekDiff != 0 || reCalculate -> {
                val playCounts = IntArray(NUM_WEEKS)
                when {
                    weekDiff > 0 -> {
                        // time is shifted forwards
                        for (i in 0 until NUM_WEEKS - weekDiff) {
                            playCounts[i + weekDiff] = cursor.getInt(getColumnIndexForWeek(i))
                        }
                    }
                    weekDiff < 0 -> {
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
                    weekDiff == 0 -> {
                        // occurs when reCalculate is true
                        for (i in 0 until NUM_WEEKS) {
                            playCounts[i] = cursor.getInt(getColumnIndexForWeek(i))
                        }
                    }
                }

                // bump the count if need
                if (bumpCount) {
                    playCounts[0]++
                }

                // calculate
                val score = calculateScore(playCounts)

                // update table
                if (score < .01f) {
                    // if the score is non-existent, then delete it
                    deleteEntry(database, songId.toString())
                } else {
                    database.update(
                        NAME,
                        ContentValues(NUM_WEEKS + 2).apply {
                            put(LAST_UPDATED_WEEK_INDEX, mCurrentWeekNumber)
                            put(PLAY_COUNT_SCORE, score)
                            for (i in 0 until NUM_WEEKS) {
                                put(getColumnNameForWeek(i), playCounts[i])
                            }
                        },
                        WHERE_ID_EQUALS, arrayOf(songId.toString())
                    )
                }
            }
            // same week, no need for shifting, so just update the scores
            weekDiff == 0 -> {
                if (bumpCount) {
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
                        WHERE_ID_EQUALS, arrayOf(songId.toString())
                    )
                } // else // do nothing!
            }
        }
    }

    /**
     * This function will take a song entry and update it to the latest week and increase the count
     * for the current week by 1 if necessary
     *
     * @param database  a writeable database
     * @param id        the id of the track to bump
     * @param bumpCount whether to bump the current's week play count by 1 and adjust the score
     */
    private fun updateExistingRow(
        database: SQLiteDatabase,
        id: Long,
        bumpCount: Boolean,
        force: Boolean = false
    ) {

        // begin the transaction
        database.beginTransaction()

        // get the cursor of this content inside the transaction
        val cursor = database.query(NAME, null, WHERE_ID_EQUALS, arrayOf(id.toString()), null, null, null)
        try {
            // if target existed
            if (cursor != null && cursor.moveToFirst()) {
                updateExistedPlayedEntry(readableDatabase, cursor, id, bumpCount, force)
            } else {
                // if we have no existing results, create a new one
                if (bumpCount) createNewPlayedEntry(database, id)
            }
            database.setTransactionSuccessful()
        } catch (e: Exception) {
            ErrorNotification.postErrorNotification(e, "Failed to update song play count in SongPlayCountDatabase (songId=$id)")
        } finally {
            cursor?.close()
            database.endTransaction()
        }
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
        return readableDatabase.query(
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
        try {
            // clean outdated weeks
            val oldestWeekWeCareAbout = mCurrentWeekNumber - NUM_WEEKS + 1
            database.delete(
                NAME,
                "$LAST_UPDATED_WEEK_INDEX < $oldestWeekWeCareAbout",
                null
            ) // delete rows we don't care about anymore

            // get the remaining rows
            database.query(
                NAME, arrayOf(ID),
                null, null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // for each row, update it
                    do {
                        updateExistingRow(database, cursor.getLong(0), false)
                    } while (cursor.moveToNext())
                }
            }
            mDatabaseUpdated = true
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    /**
     * re-calculate-score
     */
    fun reCalculateScore(context: Context) {
        readableDatabase.query(NAME, arrayOf(ID), null, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val totalCount = cursor.count
                try {
                    var i = 0
                    do {
                        i++
                        updateExistingRow(readableDatabase, cursor.getLong(0), bumpCount = false, force = true)
                        if (i.mod(31) == 0)
                            BackgroundNotification.post(
                                context.getString(R.string.refresh),
                                context.getString(R.string.my_top_tracks),
                                NOTIFICATION_ID, i, totalCount
                            )
                    } while (cursor.moveToNext())
                } catch (e: Exception) {
                    ErrorNotification.postErrorNotification(e, "${context.getString(R.string.failed)}:\n${Thread.currentThread().stackTrace}")
                } finally {
                    BackgroundNotification.remove(NOTIFICATION_ID)
                }
            }
        }
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

    fun gc(idsExists: List<Long>) {
        gc(writableDatabase, NAME, ID, idsExists.map { it.toString() }.toTypedArray())
    }

    interface SongPlayCountColumns {
        companion object {
            const val NAME = "song_play_count"
            const val ID = "song_id"
            const val WEEK = "week"
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
        private fun getColumnNameForWeek(week: Int): String = WEEK + week.toString()

        /**
         * Gets the score multiplier for each week
         *
         * @param week number
         * @return the multiplier to apply
         */
        private fun getScoreMultiplierForWeek(week: Int): Float = week * ratio[week] + 1

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

        private const val ONE_WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7
        private const val WHERE_ID_EQUALS = "$ID=?"

        /**
         * Multiplier factor
         * result of 10 * exp  ( -0.2 * sqrt(x) ), x = 0 to 51
         */
        private val ratio: FloatArray by lazy {
            floatArrayOf(
                10.0000000F,
                8.18730753F,
                7.53638316F,
                7.07222352F,
                6.70320046F,
                6.39407319F,
                6.12688916F,
                5.89105342F,
                5.67970712F,
                5.48811636F,
                5.31285609F,
                5.15135679F,
                5.00163455F,
                4.86212136F,
                4.73155364F,
                4.60889634F,
                4.49328964F,
                4.38401060F,
                4.28044491F,
                4.18206567F,
                4.08841719F,
                3.99910247F,
                3.91377327F,
                3.83212235F,
                3.75387708F,
                3.67879441F,
                3.60665658F,
                3.53726775F,
                3.47045104F,
                3.40604614F,
                3.34390731F,
                3.28390157F,
                3.22590729F,
                3.16981289F,
                3.11551576F,
                3.06292130F,
                3.01194211F,
                2.96249727F,
                2.91451169F,
                2.86791559F,
                2.82264398F,
                2.77863623F,
                2.73583571F,
                2.69418942F,
                2.65364768F,
                2.61416388F,
                2.57569421F,
                2.53819747F,
                2.50163482F,
                2.46596963F,
                2.43116734F,
                2.39719524F,
            )
        }

        fun Int.requireNotNegative(): Int = if (this < 0) throw IllegalStateException("Must be non-negative!") else this

        private const val NOTIFICATION_ID = 7727
    }
}
