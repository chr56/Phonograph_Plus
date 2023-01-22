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
package player.phonograph.mediastore.internal

import android.database.AbstractCursor
import android.database.Cursor

/**
 * This cursor basically wraps a song cursor and is given a list of the order of the ids of the
 * contents of the cursor. It wraps the Cursor and simulates the internal cursor being sorted
 * by moving the point to the appropriate spot
 */
class SortedCursor : AbstractCursor {

    /**
     * @param cursor     to wrap
     * @param order      the list of unique ids in sorted order to display
     * @param columnName the column name of the id to look up in the internal cursor
     */
    constructor(cursor: Cursor, order: Array<String>?, columnName: String) : super() {
        this.mCursor = cursor
        this.mMissingValues = buildCursorPositionMapping(order, columnName)
    }

    // cursor to wrap
    private val mCursor: Cursor

    // this contains the ids that weren't found in the underlying cursor
    var mMissingValues: List<String>
        private set

    // the map of external indices to internal indices
    private lateinit var mOrderedPositions: MutableList<Int>

    // this contains the mapped cursor positions and afterwards the extra ids that weren't found
    private lateinit var mMapCursorPositions: java.util.HashMap<String, Int>

    /**
     * This function populates mOrderedPositions with the cursor positions in the order based
     * on the order passed in
     *
     * @param order the target order of the internal cursor
     * @return returns the ids that aren't found in the underlying cursor
     */
    private fun buildCursorPositionMapping(order: Array<String>?, columnName: String): List<String> {
        val missingValues: MutableList<String> = ArrayList()
        mOrderedPositions = ArrayList(mCursor.count)
        mMapCursorPositions = HashMap(mCursor.count)
        val valueColumnIndex = mCursor.getColumnIndex(columnName)
        if (mCursor.moveToFirst()) {
            // first figure out where each of the ids are in the cursor
            do {
                mMapCursorPositions[mCursor.getString(valueColumnIndex)] = mCursor.position
            } while (mCursor.moveToNext())
            if (order != null) {
                // now create the ordered positions to map to the internal cursor given the
                // external sort order
                for (value in order) {
                    if (mMapCursorPositions.containsKey(value)) {
                        mOrderedPositions.add(mMapCursorPositions[value]!!)
                        mMapCursorPositions.remove(value)
                    } else {
                        missingValues.add(value)
                    }
                }
            }
            mCursor.moveToFirst()
        }
        return missingValues
    }

    /**
     * @return the list of ids that were in the underlying cursor but not part of the ordered list
     */
    val extraValues: Collection<String> get() = mMapCursorPositions.keys

    override fun close() {
        mCursor.close()
        super.close()
    }

    override fun getCount(): Int = mOrderedPositions.size

    override fun getColumnNames(): Array<String> = mCursor.columnNames

    override fun getString(column: Int): String = mCursor.getString(column)

    override fun getShort(column: Int): Short = mCursor.getShort(column)

    override fun getInt(column: Int): Int = mCursor.getInt(column)

    override fun getLong(column: Int): Long = mCursor.getLong(column)

    override fun getFloat(column: Int): Float = mCursor.getFloat(column)

    override fun getDouble(column: Int): Double = mCursor.getDouble(column)

    override fun isNull(column: Int): Boolean = mCursor.isNull(column)

    override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
        if (newPosition in 0 until count) {
            mCursor.moveToPosition(mOrderedPositions[newPosition])
            return true
        }
        return false
    }
}
