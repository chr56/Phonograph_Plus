/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.zip

import player.phonograph.util.reportError
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {

    fun addToZipFile(destination: ZipOutputStream, file: File, entryName: String): Boolean {
        runCatching {
            if (file.exists() && file.isFile) {
                destination.putNextEntry(ZipEntry(entryName))
                BufferedInputStream(FileInputStream(file)).use { fs ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (fs.read(buffer).also { len = it } != -1) {
                        destination.write(buffer, 0, len)
                    }
                }
            } else {
                reportError(
                    IllegalStateException(), "ZopUtil",
                    "File ${file.name} is not a file"
                )
            }
        }.let {
            if (it.isFailure) reportError(
                it.exceptionOrNull() ?: Exception(),
                "ZopUtil",
                "Failed to add ${file.name} to current archive file ($destination)"
            )
            return it.isSuccess
        }
    }

    fun extractZipFile(source: ZipInputStream, destinationDir: File) {
        var entry: ZipEntry?
        while (source.nextEntry.also { entry = it } != null) {
            entry?.apply {
                if (!isDirectory) {
                    val file = File(destinationDir, name)
                    FileOutputStream(file).use { fos ->
                        BufferedOutputStream(fos).use { outputStream ->
                            var len: Int
                            val bytes = ByteArray(1024)
                            while (source.read(bytes).also { len = it } != -1) {
                                outputStream.write(bytes, 0, len)
                            }
                        }
                    }
                } else {
                    Log.w("ZopUtil", "${this.name} is directory!!")
                }
            }
        }
    }
}