/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import player.phonograph.ui.activities.AlbumDetailActivityViewModel
import player.phonograph.ui.activities.ArtistDetailActivityViewModel
import player.phonograph.ui.modules.playlist.PlaylistDetailViewModel

val moduleViewModels = module  {
    viewModel { param ->  ArtistDetailActivityViewModel(param.get()) }
    viewModel { param ->  AlbumDetailActivityViewModel(param.get()) }
    viewModel { param ->  PlaylistDetailViewModel(param.get()) }
}