/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import player.phonograph.ui.activities.ArtistDetailActivityViewModel

val moduleViewModels = module  {
    viewModel { param ->  ArtistDetailActivityViewModel(param.get()) }
}