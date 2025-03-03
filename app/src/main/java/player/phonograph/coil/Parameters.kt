/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil

import coil.request.Parameters

const val PARAMETERS_KEY_PALETTE = "palette"
const val PARAMETERS_KEY_RAW = "raw"


fun Parameters.raw(default: Boolean): Boolean = value(PARAMETERS_KEY_RAW) ?: default
fun Parameters.palette(default: Boolean): Boolean = value(PARAMETERS_KEY_PALETTE) ?: default