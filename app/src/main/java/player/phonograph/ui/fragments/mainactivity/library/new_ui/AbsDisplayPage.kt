/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import com.google.android.material.appbar.AppBarLayout
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.App
import player.phonograph.R
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.util.ViewUtil

abstract class AbsDisplayPage<A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> : AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    protected lateinit var adapter: A
    protected lateinit var layoutManager: LM

    protected abstract fun initLayoutManager(): LM
    protected abstract fun initAdapter(): A

    protected var adapterDataObserver: RecyclerView.AdapterDataObserver? = null

//    protected var outerAppbarOffsetListener =
//        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
//            binding.container.setPadding(
//                binding.container.paddingLeft,
//                binding.container.paddingTop,
//                binding.container.paddingRight,
//                hostFragment.totalAppBarScrollingRange + verticalOffset
//            )
//        }

    protected var innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.innerAppBar.totalScrollRange + verticalOffset,
                binding.container.paddingRight,
                binding.container.paddingBottom

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

//        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        val actionDrawable = AppCompatResources.getDrawable(hostFragment.mainActivity, R.drawable.ic_sort_variant_white_24dp)
        actionDrawable?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(
                binding.textPageHeader.currentTextColor,
                BlendModeCompat.SRC_IN
            )
        binding.actionPageHeader.setImageDrawable(actionDrawable)
        binding.actionPageHeader.setOnClickListener {
            if (!hostFragment.isPopupMenuInited) hostFragment.initPopup()
            configPopup(hostFragment.popupMenu, hostFragment.popup)
            Log.d("HomePage", "popupWindowVerticalOffset:")
            hostFragment.popupMenu.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0,
                (hostFragment.mainActivity.findViewById<player.phonograph.views.StatusBarView>(R.id.status_bar)?.height ?: 8) +
                    hostFragment.totalHeaderHeight + binding.innerAppBar.height
            )
        }
        binding.empty.setText(emptyMessage)
    }

    abstract fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding)

    protected open val emptyMessage: Int @StringRes get() = R.string.empty

    override fun onDestroyView() {
        super.onDestroyView()
        adapterDataObserver?.let {
            adapter.unregisterAdapterDataObserver(it)
        }
        adapterDataObserver = null

        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
//        hostFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
        _viewBinding = null
    }
}
