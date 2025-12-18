/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.mediastore

import android.database.AbstractCursor
import android.database.Cursor

/**
 * This cursor basically wraps a song cursor and is given a list of the order of the ids of the
 * contents of the cursor. It wraps the Cursor and simulates the internal cursor being sorted
 * by moving the point to the appropriate spot
 */
class SortedLongCursor : AbstractCursor {

    /**
     * @param cursor     to wrap
     * @param order      the list of unique ids in sorted order to display
     * @param columnName the column name of the id to look up in the internal cursor
     */
    constructor(cursor: Cursor, order: LongArray?, columnName: String) : super() {
        this.mCursor = cursor
        missingIds = buildCursorPositionMapping(order, columnName)
    }

    // cursor to wrap
    private val mCursor: Cursor

    // the map of external indices to internal indices
    private var mOrderedPositions: MutableList<Int?>? = null

    // this contains the ids that weren't found in the underlying cursor
    val missingIds: List<Long>

    // this contains the mapped cursor positions and afterwards the extra ids that weren't found
    private var mMapCursorPositions: HashMap<Long, Int>? = null

    /**
     * This function populates mOrderedPositions with the cursor positions in the order based
     * on the order passed in
     *
     * @param order the target order of the internal cursor
     * @return returns the ids that aren't found in the underlying cursor
     */
    private fun buildCursorPositionMapping(order: LongArray?, columnName: String): List<Long> {
        val missingIds: MutableList<Long> = ArrayList()
        mOrderedPositions = ArrayList(mCursor.count)
        mMapCursorPositions = HashMap(mCursor.count)
        val idPosition = mCursor.getColumnIndex(columnName)
        if (mCursor.moveToFirst()) {
            // first figure out where each of the ids are in the cursor
            do {
                mMapCursorPositions!![mCursor.getLong(idPosition)] = mCursor.position
            } while (mCursor.moveToNext())

            // now create the ordered positions to map to the internal cursor given the
            // external sort order
            var i = 0
            while (order != null && i < order.size) {
                val id = order[i]
                if (mMapCursorPositions!!.containsKey(id)) {
                    mOrderedPositions!!.add(mMapCursorPositions!![id])
                    mMapCursorPositions!!.remove(id)
                } else {
                    missingIds.add(id)
                }
                i++
            }
            mCursor.moveToFirst()
        }
        return missingIds
    }

    /**
     * @return the list of ids that were in the underlying cursor but not part of the ordered list
     */
    val extraIds: Collection<Long>
        get() = mMapCursorPositions!!.keys

    override fun close() {
        mCursor.close()
        super.close()
    }

    override fun getCount(): Int = mOrderedPositions!!.size

    override fun getColumnNames(): Array<String> = mCursor.columnNames

    override fun getString(column: Int): String? = mCursor.getString(column)

    override fun getShort(column: Int): Short = mCursor.getShort(column)

    override fun getInt(column: Int): Int = mCursor.getInt(column)

    override fun getLong(column: Int): Long = mCursor.getLong(column)

    override fun getFloat(column: Int): Float = mCursor.getFloat(column)

    override fun getDouble(column: Int): Double = mCursor.getDouble(column)

    override fun isNull(column: Int): Boolean = mCursor.isNull(column)

    override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
        if (newPosition in 0 until count) {
            mCursor.moveToPosition(mOrderedPositions!![newPosition]!!)
            return true
        }
        return false
    }
}