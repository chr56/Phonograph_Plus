/*
 *  Copyright (c) 2022~2023 chr_56
 */

package tools.release.zip

import tools.release.file.assureFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream


fun File.gzip(target: File? = null): File = gzipImpl(this, target)

fun gzipImpl(file: File, target: File? = null): File {
    require(file.exists()) { "File(${file.absolutePath}) is not available" }
    require(file.isFile) { "File(${file.absolutePath}) is not a file!" }
    val zippedFile = (target ?: File("${file.absolutePath}.gz")).assureFile()
    FileOutputStream(zippedFile).use { outputStream ->
        GZIPOutputStream(outputStream, 4096).use { gzipOutputStream ->
            FileInputStream(file).use { inputStream ->
                val buffer = ByteArray(4096)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    gzipOutputStream.write(buffer, 0, len)
                }
                gzipOutputStream.flush()
            }
        }
    }
    return zippedFile
}

