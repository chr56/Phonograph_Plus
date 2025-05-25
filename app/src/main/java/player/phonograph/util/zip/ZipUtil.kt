/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.zip

import okio.IOException
import player.phonograph.App
import player.phonograph.foundation.error.warning
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {

    fun zipDirectory(destination: OutputStream, directory: File): Boolean {
        val files = directory.listFiles() ?: return false
        return try {
            ZipOutputStream(destination).use { target ->
                for (file in files) {
                    addToZipFile(target, file, file.name)
                }
            }
            true
        } catch (e: IOException) {
            warning(App.instance, "ZipUtil", "Failed to zip $destination", e)
            false
        }

    }

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
                warning(App.instance, "ZipUtil", "File ${file.name} is not a file")
            }
        }.let {
            if (it.isFailure) warning(
                App.instance,
                "ZipUtil",
                "Failed to add ${file.name} to current archive file ($destination)",
                it.exceptionOrNull()
            )
            return it.isSuccess
        }
    }


    fun extractDirectory(sourceInputStream: InputStream, directory: File): Boolean {
        return try {
            ZipInputStream(sourceInputStream).use { zipIn ->
                extractZipFile(zipIn, directory)
            }
            true
        } catch (e: Exception) {
            warning(App.instance, "ZipUtil", "Failed to extract $sourceInputStream to $directory", e)
            false
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
                    Log.w("ZipUtil", "${this.name} is directory!!")
                }
            }
        }
    }
}