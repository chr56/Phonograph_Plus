/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

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