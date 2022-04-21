/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import player.phonograph.App
import player.phonograph.R
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.Util
import player.phonograph.util.ViewUtil
import util.mdcolor.pref.ThemeColor

/**
 * @param IT the model type that this fragment displays
 * @param A relevant Adapter
 * @param LM relevant LayoutManager
 */
sealed class AbsDisplayPage<IT, A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> :
    AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    abstract fun getDataSet(): List<IT>
    abstract fun loadDataSet()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadDataSet()
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    // for mini player bar
    protected var outerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.container.paddingTop,
                binding.container.paddingRight,
                hostFragment.totalAppBarScrollingRange + verticalOffset
            )
        }

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
        binding.empty.text = resources.getText(R.string.loading)

        initRecyclerView()
//        _bindingPopup = PopupWindowMainBinding.inflate(LayoutInflater.from(hostFragment.mainActivity))
        initAppBar()
    }

    protected lateinit var adapter: A
    protected lateinit var layoutManager: LM

    protected abstract fun initLayoutManager(): LM
    protected abstract fun initAdapter(): A

    protected var isRecyclerViewPrepared: Boolean = false

    protected lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver

    protected fun initRecyclerView() {

        layoutManager = initLayoutManager()
        adapter = initAdapter()

        adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
                updateHeaderText()
            }
        }
        adapter.registerAdapterDataObserver(adapterDataObserver)

        ViewUtil.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity, binding.recyclerView,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.also {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
        isRecyclerViewPrepared = true
    }

    protected fun initAppBar() {

        binding.innerAppBar.setExpanded(false)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        val actionDrawable = AppCompatResources.getDrawable(
            hostFragment.mainActivity,
            R.drawable.ic_sort_variant_white_24dp
        )
        actionDrawable?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(
                binding.textPageHeader.currentTextColor,
                BlendModeCompat.SRC_IN
            )
        binding.buttonPageHeader.setImageDrawable(actionDrawable)
        binding.buttonPageHeader.setOnClickListener { onPopupShow() }

        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    // all pages share/re-used one popup on host fragment
    val popupWindow: PopupWindow
        get() {
            if (hostFragment.displayPopup.get() == null) {
                hostFragment.displayPopup = WeakReference(createPopup())
            }
            return hostFragment.displayPopup.get()!!
        }
    private val _bindingPopup: PopupWindowMainBinding?
        get() {
            if (hostFragment.displayPopupView.get() == null)
                hostFragment.displayPopupView =
                    WeakReference(PopupWindowMainBinding.inflate(LayoutInflater.from(hostFragment.mainActivity)))
            return hostFragment.displayPopupView.get()!!
        }
    val popupView get() = _bindingPopup!!

    protected fun createPopup(): PopupWindow {
        val mainActivity = hostFragment.mainActivity // context

        // todo move to util or view model
        val accentColor = ThemeColor.accentColor(mainActivity)
        val textColor = ThemeColor.textColorSecondary(mainActivity)
        val widgetColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
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
            for (i in 0 until this.gridSize.childCount) (this.gridSize.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
            for (i in 0 until this.sortOrderContent.childCount) (this.sortOrderContent.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
            for (i in 0 until this.sortOrderBasic.childCount) (this.sortOrderBasic.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
        }

        return PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            this.animationStyle = android.R.style.Animation_Dialog
            this.setBackgroundDrawable(
                ColorDrawable(
                    PhonographColorUtil.getCorrectBackgroundColor(
                        mainActivity
                    )
                )
            )
        }
    }

    protected fun resetPopupMenuBackgroundColor() {
        popupWindow.setBackgroundDrawable(
            ColorDrawable(
                PhonographColorUtil.getCorrectBackgroundColor(
                    hostFragment.mainActivity
                )
            )
        )
    }

    protected fun onPopupShow() {
        // first, hide all items
        hideAllPopupItems()

        // display available items
        configPopup(popupWindow, popupView)
        popupWindow.setOnDismissListener(initOnDismissListener(popupWindow, popupView))

        // show popup
        popupWindow.showAtLocation(
            binding.root, Gravity.TOP or Gravity.END, 0,
            (
                hostFragment.mainActivity.findViewById<player.phonograph.views.StatusBarView>(R.id.status_bar)?.height
                    ?: 8
                ) +
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

    abstract fun initOnDismissListener(
        popupMenu: PopupWindow,
        popup: PopupWindowMainBinding
    ): PopupWindow.OnDismissListener?

    abstract fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding)

    protected open val emptyMessage: Int @StringRes get() = R.string.empty
    protected fun checkEmpty() {
        if (isRecyclerViewPrepared) {
            binding.empty.setText(emptyMessage)
            binding.empty.visibility = if (getDataSet().isEmpty()) View.VISIBLE else View.GONE
        }
    }

    protected fun updateHeaderText() {
        binding.textPageHeader.text = getHeaderText()
    }

    protected abstract fun getHeaderText(): CharSequence

    override fun onMediaStoreChanged() {
        loadDataSet()
        super.onMediaStoreChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.unregisterAdapterDataObserver(adapterDataObserver)

        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        hostFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
//        _bindingPopup = null
        _viewBinding = null
    }

    protected val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
}

class DisplayUtil(private val page: AbsDisplayPage<*, *, *>) {
    private val isLandscape: Boolean
        get() = Util.isLandscape(page.resources)

    val maxGridSize: Int
        get() = if (isLandscape) App.instance.resources.getInteger(R.integer.max_columns_land) else
            App.instance.resources.getInteger(R.integer.max_columns)
    val maxGridSizeForList: Int
        get() = if (isLandscape) App.instance.resources.getInteger(R.integer.default_list_columns_land) else
            App.instance.resources.getInteger(R.integer.default_list_columns)

    var sortMode: SortMode
        get() {
            val pref = Setting.instance
            return when (page) {
                is AlbumPage -> {
                    pref.albumSortMode
                }
                else -> SortMode(SortRef.ID)
            }
        }
        set(value) {
            val pref = Setting.instance
            when (page) {
                is AlbumPage -> {
                    pref.albumSortMode = value
                }
                else -> {}
            }
        }

    var sortOrder: String
        get() {
            val pref = Setting.instance
            return when (page) {
                is SongPage -> {
                    pref.songSortOrder
                }
                is AlbumPage -> {
                    pref.albumSortOrder
                }
                is ArtistPage -> {
                    pref.artistSortOrder
                }
                is GenrePage -> {
                    pref.genreSortOrder
                }
                else -> ""
            }
        }
        set(value) {
            if (value.isBlank()) return

            val pref = Setting.instance
            // todo valid input
            when (page) {
                is SongPage -> {
                    pref.songSortOrder = value
                }
                is AlbumPage -> {
                    pref.albumSortOrder = value
                }
                is ArtistPage -> {
                    pref.artistSortOrder = value
                }
                is GenrePage -> {
                    pref.genreSortOrder = value
                }
            }
        }

    var gridSize: Int
        get() {
            val pref = Setting.instance

            return when (page) {
                is SongPage -> {
                    if (isLandscape) pref.songGridSizeLand
                    else pref.songGridSize
                }
                is AlbumPage -> {
                    if (isLandscape) pref.albumGridSizeLand
                    else pref.albumGridSize
                }
                is ArtistPage -> {
                    if (isLandscape) pref.artistGridSizeLand
                    else pref.artistGridSize
                }
                is GenrePage -> {
                    if (isLandscape) pref.genreGridSizeLand
                    else pref.genreGridSize
                }
                else -> 1
            }
        }
        set(value) {
            if (value <= 0) return
            val pref = Setting.instance
            // todo valid input
            when (page) {
                is SongPage -> {
                    if (isLandscape) pref.songGridSizeLand = value
                    else pref.songGridSize = value
                }
                is AlbumPage -> {
                    if (isLandscape) pref.albumGridSizeLand = value
                    else pref.albumGridSize = value
                }
                is ArtistPage -> {
                    if (isLandscape) pref.artistGridSizeLand = value
                    else pref.artistGridSize = value
                }
                is GenrePage -> {
                    if (isLandscape) pref.genreGridSizeLand = value
                    else pref.genreGridSize = value
                }
            }
        }
    var colorFooter: Boolean
        get() {
            val pref = Setting.instance
            return when (page) {
                is SongPage -> {
                    pref.songColoredFooters
                }
                is AlbumPage -> {
                    pref.albumColoredFooters
                }
                is ArtistPage -> {
                    pref.artistColoredFooters
                }
                else -> false
            }
        }
        set(value) {
            val pref = Setting.instance
            // todo valid input
            when (page) {
                is SongPage -> {
                    pref.songColoredFooters = value
                }
                is AlbumPage -> {
                    pref.albumColoredFooters = value
                }
                is ArtistPage -> {
                    pref.artistColoredFooters = value
                }
                is GenrePage -> {
                    // do noting
                }
            }
        }
}
