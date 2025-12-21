/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model

import java.io.StringWriter
import java.io.Writer

interface OutputFormat {
    fun write(target: Writer)
    fun write(): String {
        val buffer = StringWriter()
        write(buffer)
        return buffer.toString()
    }
}