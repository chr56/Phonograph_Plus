/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.file

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ArchiverResult(
    val isSuccess: Boolean,
    val message: String? = null,
    val errors: List<Throwable> = emptyList(),
) {
    fun onError(block: (String, List<Throwable>) -> Unit): ArchiverResult {
        if (!isSuccess && message != null) block(message, errors)
        return this
    }
}

fun compressDirectory(destination: OutputStream, directory: File): ArchiverResult {
    val files = directory.listFiles() ?: return ArchiverResult(false)
    val errors = mutableListOf<Throwable>()
    return try {
        ZipOutputStream(destination).use { target ->
            for (file in files) {
                addToZipFile(target, file, file.name).onError { _, e ->
                    errors += e
                }
            }
        }
        ArchiverResult(true)
    } catch (e: IOException) {
        ArchiverResult(false, "Failed to compress $directory", listOf(e) + errors)
    }
}


private fun addToZipFile(destination: ZipOutputStream, file: File, entryName: String): ArchiverResult = try {
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
        Log.w("FileArchive", "File ${file.name} is not a file")
    }
    ArchiverResult(true)
} catch (e: Exception) {
    ArchiverResult(false, "Failed to add ${file.name} to archive", listOf(e))
}


fun extractDirectory(sourceInputStream: InputStream, directory: File): ArchiverResult = try {
    ZipInputStream(sourceInputStream).use { zipIn ->
        extractZipFile(zipIn, directory)
    }
    ArchiverResult(true)
} catch (e: Exception) {
    ArchiverResult(false, "Failed to extract $sourceInputStream to $directory", listOf(e))
}

private fun extractZipFile(source: ZipInputStream, destinationDir: File) {
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
                Log.w("FileArchive", "${this.name} is directory!!")
            }
        }
    }
}
