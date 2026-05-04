/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.retriever

import coil.fetch.FetchResult
import coil.size.Size
import android.content.Context

interface ImageRetriever {
    val name: String
    fun retrieve(
        path: String,
        id: Long,
        context: Context,
        size: Size,
        raw: Boolean,
    ): FetchResult?

    val id: Int
}