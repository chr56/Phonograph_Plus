/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.metadata.read

/**
 * Indicate low-level exception when extracting metadata
 */
class MetadataExtractingException : Exception {
    constructor(message: String) : super(message)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}