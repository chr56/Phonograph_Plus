package player.phonograph.provider

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import player.phonograph.BaseApp
import player.phonograph.MEDIA_STORE_CHANGED
import player.phonograph.settings.Setting
import player.phonograph.util.FileUtil.safeGetCanonicalPath
import java.io.File

class BlacklistStore(context: Context) :
    SQLiteOpenHelper(context, DatabaseConstants.BLACKLIST_DB, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS ${BlacklistStoreColumns.NAME} (${BlacklistStoreColumns.PATH} TEXT NOT NULL);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + BlacklistStoreColumns.NAME)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + BlacklistStoreColumns.NAME)
        onCreate(db)
    }

    fun addPath(file: File?) {
        addPathImpl(file)
        notifyMediaStoreChanged()
    }

    private fun addPathImpl(file: File?) {
        if (file == null || contains(file)) return

        val path = safeGetCanonicalPath(file)

        with(writableDatabase) {
            beginTransaction()
            try {
                // add the entry
                insert(
                    BlacklistStoreColumns.NAME, null,
                    ContentValues(1).apply {
                        put(BlacklistStoreColumns.PATH, path)
                    }
                )
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    operator fun contains(file: File?): Boolean {
        if (file == null) return false

        val path = safeGetCanonicalPath(file)
        return readableDatabase.query(
            BlacklistStoreColumns.NAME, arrayOf(BlacklistStoreColumns.PATH),
            BlacklistStoreColumns.PATH + "=?", arrayOf(path),
            null, null, null, null
        )?.use {
            it.moveToFirst()
        } ?: false
    }

    fun removePath(file: File) {
        writableDatabase.delete(
            BlacklistStoreColumns.NAME,
            BlacklistStoreColumns.PATH + "=?", arrayOf(safeGetCanonicalPath(file))
        )
        notifyMediaStoreChanged()
    }

    fun clear() {
        writableDatabase.delete(BlacklistStoreColumns.NAME, null, null)
        notifyMediaStoreChanged()
    }

    private fun notifyMediaStoreChanged() {
        BaseApp.instance.sendBroadcast(Intent(MEDIA_STORE_CHANGED))
    }

    val paths: List<String>
        get() {
            val paths: MutableList<String> = ArrayList()
            readableDatabase.query(
                BlacklistStoreColumns.NAME, arrayOf(BlacklistStoreColumns.PATH),
                null, null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        paths.add(cursor.getString(0))
                    } while (cursor.moveToNext())
                }
            }
            return paths
        }

    interface BlacklistStoreColumns {
        companion object {
            const val NAME = "blacklist"
            const val PATH = "path"
        }
    }

    companion object {
        private var sInstance: BlacklistStore? = null

        @Synchronized
        fun getInstance(context: Context): BlacklistStore {
            return sInstance ?: BlacklistStore(context.applicationContext).also { blacklistStore ->
                sInstance = blacklistStore
                if (!Setting.instance.initializedBlacklist) {
                    // blacklisted by default
                    blacklistStore.addPathImpl(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS))
                    blacklistStore.addPathImpl(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS))
                    blacklistStore.addPathImpl(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES))
                    Setting.instance.initializedBlacklist = true
                }
            }
        }
        private const val VERSION = 1
    }
}
