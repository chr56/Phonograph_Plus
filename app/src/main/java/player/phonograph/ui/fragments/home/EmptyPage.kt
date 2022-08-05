/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import player.phonograph.databinding.FragmentMainActivityRecyclerViewBinding

class EmptyPage : AbsPage() {
    private var _viewBinding: FragmentMainActivityRecyclerViewBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentMainActivityRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }
}
