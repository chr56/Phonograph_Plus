/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import player.phonograph.ui.activities.base.PanelViewModel
import player.phonograph.ui.modules.album.AlbumDetailActivityViewModel
import player.phonograph.ui.modules.artist.ArtistDetailActivityViewModel
import player.phonograph.ui.modules.playlist.PlaylistDetailViewModel

val moduleViewModels = module {
    viewModel { param -> PanelViewModel(param.get(), param.get(), param.get()) }
    viewModel { param -> ArtistDetailActivityViewModel(param.get()) }
    viewModel { param -> AlbumDetailActivityViewModel(param.get()) }
    viewModel { param -> PlaylistDetailViewModel(param.get(), param.get()) }
}