package player.phonograph.provider

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import player.phonograph.MusicServiceMsgConst
import java.io.File
import player.phonograph.App
import player.phonograph.settings.Setting
import player.phonograph.util.FileUtil.safeGetCanonicalPath

class PathFilterStore(context: Context) :
    SQLiteOpenHelper(context, DatabaseConstants.PATH_FILTER, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_BLACKLIST (${PATH} TEXT NOT NULL);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BLACKLIST")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BLACKLIST")
        onCreate(db)
    }

    fun addBlacklistPath(file: File) {
        addBlacklistPathImpl(file)
        notifyMediaStoreChanged()
    }

    private fun addBlacklistPathImpl(file: File?) {
        if (file == null || containsBlacklist(file)) return

        val path = safeGetCanonicalPath(file)

        with(writableDatabase) {
            beginTransaction()
            try {
                // add the entry
                insert(
                    TABLE_BLACKLIST, null,
                    ContentValues(1).apply {
                        put(PATH, path)
                    }
                )
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    fun containsBlacklist(file: File?): Boolean {
        if (file == null) return false

        val path = safeGetCanonicalPath(file)
        return readableDatabase.query(
            TABLE_BLACKLIST, arrayOf(PATH),
            "$PATH = ?", arrayOf(path),
            null, null, null, null
        )?.use {
            it.moveToFirst()
        } ?: false
    }

    fun removeBlacklistPath(file: File) {
        writableDatabase.delete(
            TABLE_BLACKLIST,
            "$PATH = ?", arrayOf(safeGetCanonicalPath(file))
        )
        notifyMediaStoreChanged()
    }

    fun clearBlacklist() {
        writableDatabase.delete(TABLE_BLACKLIST, null, null)
        notifyMediaStoreChanged()
    }


    val blacklistPaths: List<String>
        get() {
            val paths: MutableList<String> = ArrayList()
            readableDatabase.query(
                TABLE_BLACKLIST, arrayOf(PATH),
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


    companion object {
        private const val VERSION = 1

        const val TABLE_BLACKLIST = "blacklist"
        const val PATH = "path"

        private var sInstance: PathFilterStore? = null

        @Synchronized
        fun getInstance(context: Context): PathFilterStore {
            return sInstance ?: PathFilterStore(context.applicationContext).also { blacklistStore ->
                sInstance = blacklistStore
                if (!Setting.instance.initializedBlacklist) {
                    // blacklisted by default
                    blacklistStore.addBlacklistPathImpl(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS))
                    blacklistStore.addBlacklistPathImpl(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS))
                    blacklistStore.addBlacklistPathImpl(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES))
                    Setting.instance.initializedBlacklist = true
                }
            }
        }


        private fun notifyMediaStoreChanged() {
            App.instance.sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))
        }
    }
}
