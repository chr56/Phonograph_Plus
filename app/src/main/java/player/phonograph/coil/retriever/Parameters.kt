/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.request.Options
import coil.request.Parameters


const val PARA_KEY_RAW = "raw"
val PARAMETERS_RAW = Parameters.Builder().set(PARA_KEY_RAW, true).build()


internal fun Options.raw(default: Boolean): Boolean {
    return parameters.value(PARA_KEY_RAW) ?: default
}