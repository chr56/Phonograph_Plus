/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.file

import android.webkit.MimeTypeMap
import java.io.File

fun File.mimeTypeIs(mimeType: String): Boolean {
    return if (mimeType.isEmpty() || mimeType == "*/*") {
        true
    } else {
        // get the file mime type
        val filename = this.toURI().toString()
        val dotPos = filename.lastIndexOf('.')
        if (dotPos == -1) {
            return false
        }
        val fileExtension = filename.substring(dotPos + 1).lowercase()
        val fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension) ?: return false
        // check the 'type/subtype' pattern
        if (fileType == mimeType) {
            return true
        }
        // check the 'type/*' pattern
        val mimeTypeDelimiter = mimeType.lastIndexOf('/')
        if (mimeTypeDelimiter == -1) {
            return false
        }
        val mimeTypeMainType = mimeType.substring(0, mimeTypeDelimiter)
        val mimeTypeSubtype = mimeType.substring(mimeTypeDelimiter + 1)
        if (mimeTypeSubtype != "*") {
            return false
        }
        val fileTypeDelimiter = fileType.lastIndexOf('/')
        if (fileTypeDelimiter == -1) {
            return false
        }
        val fileTypeMainType = fileType.substring(0, fileTypeDelimiter)
        fileTypeMainType == mimeTypeMainType
    }
}