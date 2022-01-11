/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.annotation.ColorInt
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
import player.phonograph.util.PreferenceUtil
import player.phonograph.util.Util
import player.phonograph.util.ViewUtil
import java.lang.ref.WeakReference

sealed class AbsDisplayPage<A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> : AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

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

        initViewPage()
        _bindingPopup = PopupWindowMainBinding.inflate(LayoutInflater.from(hostFragment.mainActivity))
        initAppBar()

    }

    protected lateinit var adapter: A
    protected lateinit var layoutManager: LM

    protected abstract fun initLayoutManager(): LM
    protected abstract fun initAdapter(): A

    protected var isRecyclerViewPrepared: Boolean = false

    protected var adapterDataObserver: RecyclerView.AdapterDataObserver? = null

    protected fun initViewPage() {

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
        isRecyclerViewPrepared = true

        binding.empty.setText(emptyMessage)
        // todo
    }

    protected fun initAppBar() {

        binding.innerAppBar.setExpanded(false)
//        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        val actionDrawable = AppCompatResources.getDrawable(hostFragment.mainActivity, R.drawable.ic_sort_variant_white_24dp)
        actionDrawable?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(
                binding.textPageHeader.currentTextColor,
                BlendModeCompat.SRC_IN
            )
        binding.actionPageHeader.setImageDrawable(actionDrawable)
        binding.actionPageHeader.setOnClickListener { onPopupShow() }
    }

    // all pages share/re-used one popup on host fragment
    val popupWindow: PopupWindow
        get() {
            if (hostFragment.displayPopup.get() == null) {
                hostFragment.displayPopup = WeakReference(createPopup())
            }
            return hostFragment.displayPopup.get()!!
        }
    private var _bindingPopup: PopupWindowMainBinding? = null
    val popupView get() = _bindingPopup!!

    protected fun createPopup(): PopupWindow {
        val mainActivity = hostFragment.mainActivity // context

        // todo move to util or view model
        val accentColor = ThemeColor.accentColor(mainActivity)
        val textColor = ThemeColor.textColorSecondary(mainActivity)
        val widgetColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(accentColor, accentColor, textColor)
        )
        //
        // init content color
        //
        popupView.apply {
            // text color
            this.textGridSize.setTextColor(accentColor)
            this.textSortOrderBasic.setTextColor(accentColor)
            this.textSortOrderContent.setTextColor(accentColor)
            // checkbox color
            this.actionColoredFooters.buttonTintList = widgetColor
            // radioButton
            for (i in 0 until this.gridSize.childCount) (this.gridSize.getChildAt(i) as RadioButton).buttonTintList = widgetColor
            for (i in 0 until this.sortOrderContent.childCount) (this.sortOrderContent.getChildAt(i) as RadioButton).buttonTintList = widgetColor
            for (i in 0 until this.sortOrderBasic.childCount) (this.sortOrderBasic.getChildAt(i) as RadioButton).buttonTintList = widgetColor
        }

        return PopupWindow(popupView.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            this.animationStyle = android.R.style.Animation_Dialog
            this.setBackgroundDrawable(ColorDrawable(getCorrectBackgroundColor(mainActivity)))
        }
    }
    protected fun resetPopupMenuBackgroundColor() { popupWindow.setBackgroundDrawable(ColorDrawable(getCorrectBackgroundColor(hostFragment.mainActivity))) }

    protected fun onPopupShow() {
        // first, hide all items
        hideAllPopupItems()

        // display available items
        configPopup(popupWindow, popupView)
        popupWindow.setOnDismissListener(initOnDismissListener(popupWindow, popupView))

        // show popup
        popupWindow.showAtLocation(
            binding.root, Gravity.TOP or Gravity.END, 0,
            (hostFragment.mainActivity.findViewById<player.phonograph.views.StatusBarView>(R.id.status_bar)?.height ?: 8) +
                hostFragment.totalHeaderHeight + binding.innerAppBar.height
        )

        // then valid background color
        resetPopupMenuBackgroundColor()
    }

    protected fun hideAllPopupItems() {
        popupView.sortOrderBasic.visibility = View.GONE
        popupView.sortOrderBasic.clearCheck()
        popupView.textSortOrderBasic.visibility = View.GONE

        popupView.sortOrderContent.visibility = View.GONE
        popupView.sortOrderContent.clearCheck()
        popupView.textSortOrderContent.visibility = View.GONE

        popupView.textGridSize.visibility = View.GONE
        popupView.gridSize.clearCheck()
        popupView.gridSize.visibility = View.GONE

        popupView.actionColoredFooters.visibility = View.GONE
    }

    abstract fun initOnDismissListener(popupMenu: PopupWindow, popup: PopupWindowMainBinding): PopupWindow.OnDismissListener?

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
        _bindingPopup = null
        _viewBinding = null
    }

    @ColorInt
    protected fun getCorrectBackgroundColor(context: Context): Int { // todo move to util or view model
        return context.resources.getColor(
            when (PreferenceUtil.getInstance(context).generalTheme) {
                R.style.Theme_Phonograph_Auto -> R.color.cardBackgroundColor
                R.style.Theme_Phonograph_Light -> R.color.md_white_1000
                R.style.Theme_Phonograph_Black -> R.color.md_black_1000
                R.style.Theme_Phonograph_Dark -> R.color.md_grey_800
                else -> R.color.md_grey_700
            },
            context.theme
        )
    }
}

class DisplayUtil(private val page: AbsDisplayPage<*, *>) {
    private val isLandscape: Boolean
        get() = Util.isLandscape(page.resources)

    val maxGridSize: Int
        get() = if (isLandscape) App.instance.resources.getInteger(R.integer.max_columns_land) else
            App.instance.resources.getInteger(R.integer.max_columns)
    val maxGridSizeForList: Int
        get() = if (isLandscape) App.instance.resources.getInteger(R.integer.default_list_columns_land) else
            App.instance.resources.getInteger(R.integer.default_list_columns)

    var sortOrder: String
        get() {
            val pref = PreferenceUtil.getInstance(App.instance)
            return when (page) {
                is SongPage -> {
                    pref.songSortOrder
                }
                else -> { "" }
            }
        }
        set(value) {
            if (value.isBlank()) return

            val pref = PreferenceUtil.getInstance(App.instance)
            // todo valid input
            when (page) {
                is SongPage -> {
                    pref.songSortOrder = value
                }
                else -> {}
            }
        }

    var gridSize: Int
        get() {
            val pref = PreferenceUtil.getInstance(App.instance)

            return when (page) {
                is SongPage -> {
                    if (isLandscape) pref.songGridSizeLand
                    else pref.songGridSize
                }
                else -> 1
            }
        }
        set(value) {
            if (value <= 0) return
            val pref = PreferenceUtil.getInstance(App.instance)
            // todo valid input
            when (page) {
                is SongPage -> {
                    if (isLandscape) pref.songGridSizeLand = value
                    else pref.songGridSize = value
                }
                else -> {}
            }
        }
    var colorFooter: Boolean
        get() {
            val pref = PreferenceUtil.getInstance(App.instance)
            return when (page) {
                is SongPage -> {
                    pref.songColoredFooters()
                }
                else -> false
            }
        }
        set(value) {
            val pref = PreferenceUtil.getInstance(App.instance)
            // todo valid input
            when (page) {
                is SongPage -> {
                    pref.setSongColoredFooters(value)
                }
                else -> {}
            }
        }
}
