/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util

import android.os.Build
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Objects

@Throws(IOException::class)
fun InputStream.transferToOutputStream(outputStream: OutputStream): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        transferTo(outputStream)
    } else {
        Objects.requireNonNull(outputStream, "out")
        var transferred: Long = 0
        val buffer = ByteArray(8192)
        var read: Int
        while (this.read(buffer, 0, 8192).also { read = it } >= 0) {
            outputStream.write(buffer, 0, read)
            transferred += read.toLong()
        }
        transferred
    }
}