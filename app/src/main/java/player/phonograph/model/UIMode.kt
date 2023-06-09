/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.model

sealed class UIMode {
    object Common : UIMode()
    object Editor : UIMode()
    object Search : UIMode()
}