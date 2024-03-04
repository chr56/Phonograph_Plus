/*
 *  Copyright (c) 2022~2024 chr_56
 */

package tools.release.file

import java.io.File

fun File.assureFile(): File {
    if (!exists()) createNewFile()
    require(this.isFile) { "File($absolutePath) is not a file" }
    return this
}

fun File.assureDir(): File {
    if (!exists()) mkdirs()
    require(isDirectory) { "$name not a directory!" }
    return this
}