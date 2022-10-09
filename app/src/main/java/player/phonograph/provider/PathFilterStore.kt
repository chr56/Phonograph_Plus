package player.phonograph.provider

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment.DIRECTORY_ALARMS
import android.os.Environment.DIRECTORY_NOTIFICATIONS
import android.os.Environment.DIRECTORY_RINGTONES
import android.os.Environment.getExternalStoragePublicDirectory
import player.phonograph.App
import player.phonograph.MusicServiceMsgConst
import player.phonograph.settings.Setting
import player.phonograph.util.FileUtil.safeGetCanonicalPath
import java.io.File

class PathFilterStore(context: Context) :
    SQLiteOpenHelper(context, DatabaseConstants.PATH_FILTER, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_BLACKLIST (${PATH} TEXT NOT NULL);"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_WHITELIST (${PATH} TEXT NOT NULL);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    val blacklistPaths: List<String>
        get() = fetch(TABLE_BLACKLIST)

    val whitelistPaths: List<String>
        get() = fetch(TABLE_WHITELIST)

    private fun fetch(table: String): List<String> {
        val paths: MutableList<String> = ArrayList()
        readableDatabase.query(
            table, arrayOf(PATH), null, null, null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    paths.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
        }
        return paths
    }


    fun addBlacklistPath(file: File) {
        addPathImpl(file, TABLE_BLACKLIST)
        notifyMediaStoreChanged()
    }

    fun addWhitelistPath(file: File) {
        addPathImpl(file, TABLE_WHITELIST)
        notifyMediaStoreChanged()
    }


    fun removeBlacklistPath(file: File) {
        removePathImpl(file, TABLE_BLACKLIST)
        notifyMediaStoreChanged()
    }
    fun removeWhitelistPath(file: File) {
        removePathImpl(file, TABLE_WHITELIST)
        notifyMediaStoreChanged()
    }

    fun clearBlacklist() {
        clearTable(TABLE_BLACKLIST)
        notifyMediaStoreChanged()
    }

    fun clearWhitelist() {
        clearTable(TABLE_WHITELIST)
        notifyMediaStoreChanged()
    }


    private fun addPathImpl(file: File, table: String) {
        if (contains(file, table)) return

        val path = safeGetCanonicalPath(file)

        with(writableDatabase) {
            beginTransaction()
            try {
                // add the entry
                insert(
                    table, null,
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

    private fun removePathImpl(file: File, table: String) {
        writableDatabase.delete(
            table, "$PATH = ?", arrayOf(safeGetCanonicalPath(file))
        )
    }

    internal fun contains(file: File?, table: String): Boolean {
        if (file == null) return false

        val path = safeGetCanonicalPath(file)
        return readableDatabase.query(
            table, arrayOf(PATH),
            "$PATH = ?", arrayOf(path),
            null, null, null, null
        )?.use {
            it.moveToFirst()
        } ?: false
    }


    private fun clearTable(table: String) {
        writableDatabase.delete(table, null, null)
    }


    companion object {
        private const val VERSION = 1

        const val TABLE_BLACKLIST = "blacklist"
        const val TABLE_WHITELIST = "whitelist"
        const val PATH = "path"

        private var sInstance: PathFilterStore? = null

        @Synchronized
        fun getInstance(context: Context): PathFilterStore {
            return sInstance ?: PathFilterStore(context.applicationContext).also { blacklistStore ->
                sInstance = blacklistStore
                if (!Setting.instance.initializedBlacklist) {
                    // blacklisted by default
                    blacklistStore.addPathImpl(getExternalStoragePublicDirectory(DIRECTORY_ALARMS), TABLE_BLACKLIST)
                    blacklistStore.addPathImpl(getExternalStoragePublicDirectory(DIRECTORY_NOTIFICATIONS), TABLE_BLACKLIST)
                    blacklistStore.addPathImpl(getExternalStoragePublicDirectory(DIRECTORY_RINGTONES), TABLE_BLACKLIST)
                    Setting.instance.initializedBlacklist = true
                }
            }
        }


        private fun notifyMediaStoreChanged() {
            App.instance.sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))
        }
    }
}
