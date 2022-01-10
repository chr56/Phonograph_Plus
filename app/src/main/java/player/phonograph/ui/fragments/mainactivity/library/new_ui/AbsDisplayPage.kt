/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import com.google.android.material.appbar.AppBarLayout
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.App
import player.phonograph.R
import player.phonograph.databinding.FragmentMainActivityRecyclerViewBinding
import player.phonograph.util.ViewUtil

abstract class AbsDisplayPage<A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> :
    AbsPage(), AppBarLayout.OnOffsetChangedListener {

    private var _viewBinding: FragmentMainActivityRecyclerViewBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentMainActivityRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    protected lateinit var adapter: A
    protected lateinit var layoutManager: LM

    protected abstract fun initLayoutManager(): LM
    protected abstract fun initAdapter(): A

    protected var adapterDataObserver: RecyclerView.AdapterDataObserver? = null

    protected var onOffsetChangedListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.container.paddingTop,
                binding.container.paddingRight,
                hostFragment.totalAppBarScrollingRange + verticalOffset
            )
        }

//    protected abstract fun

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutManager = initLayoutManager()
        adapter = initAdapter()
        adapterDataObserver?.let { adapter.registerAdapterDataObserver(it) }

        ViewUtil.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity, binding.recyclerView as FastScrollRecyclerView,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.also {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }

        hostFragment.addOnAppBarOffsetChangedListener(onOffsetChangedListener)
        binding.empty.setText(emptyMessage)
    }

    protected open val emptyMessage: Int @StringRes get() = R.string.empty

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        binding.container.setPadding(
            binding.container.paddingLeft,
            binding.container.paddingTop,
            binding.container.paddingRight,
            hostFragment.totalAppBarScrollingRange + i
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapterDataObserver?.let {
            adapter.unregisterAdapterDataObserver(it)
        }
        adapterDataObserver = null

        hostFragment.removeOnAppBarOffsetChangedListener(onOffsetChangedListener)
        _viewBinding = null
    }
}
