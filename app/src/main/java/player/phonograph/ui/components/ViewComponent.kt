/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components

interface ViewComponent<C, M> {
    fun create(container: C, model: M)
    fun destroy()
}