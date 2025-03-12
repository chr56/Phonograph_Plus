/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil

import coil.request.Parameters

const val PARAMETERS_KEY_PALETTE = "palette"
const val PARAMETERS_KEY_RAW = "raw"
const val PARAMETERS_KEY_QUICK_CACHE = "quick_cache"

const val PARAMETERS_KEY_IMAGE_SOURCE_CONFIG = "image_source"


fun Parameters.raw(default: Boolean): Boolean = value(PARAMETERS_KEY_RAW) ?: default
fun Parameters.palette(default: Boolean): Boolean = value(PARAMETERS_KEY_PALETTE) ?: default
fun Parameters.quickCache(default: Boolean): Boolean = value(PARAMETERS_KEY_QUICK_CACHE) ?: default