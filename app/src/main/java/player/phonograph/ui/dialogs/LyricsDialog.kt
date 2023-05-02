/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.google.android.material.chip.Chip
import lib.phonograph.dialog.LargeDialog
import mt.pref.ThemeColor
import mt.util.color.lightenColor
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.LyricsAdapter
import player.phonograph.databinding.DialogLyricsBinding
import player.phonograph.misc.MusicProgressViewUpdateHelper
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.DEFAULT_TITLE
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.model.lyrics.LyricsSource
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.ui.fragments.player.LyricsViewModel
import player.phonograph.util.theme.nightMode
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton

/**
 * Large Dialog to show Lyrics.
 *
 * **MUST** be created from a view-model owner possessing [LyricsViewModel]
 */
class LyricsDialog : LargeDialog(), MusicProgressViewUpdateHelper.Callback {

    private var _viewBinding: DialogLyricsBinding? = null
    val binding: DialogLyricsBinding get() = _viewBinding!!

    private val viewModel: LyricsViewModel by viewModels({ requireParentFragment() })

    private val lyricsInfo: LyricsInfo get() = viewModel.lyricsInfo.value
    private var activated: AbsLyrics
        get() = lyricsInfo.activatedLyrics ?: lyricsInfo.first()
        set(value) {
            viewModel.forceReplaceLyrics(value)
        }

    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = DialogLyricsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.title.text =
            if (activated.getTitle() != DEFAULT_TITLE) activated.getTitle() else lyricsInfo.linkedSong.title
        initChip()
        initRecycleView(activated)

        // corner
        requireDialog().window!!.setBackgroundDrawable(
            GradientDrawable().apply {
                this.cornerRadius = 0f
                setColor(
                    requireContext().theme.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorBackgroundFloating))
                        .getColor(0, 0)
                )
            }
        )
        binding.ok.setOnClickListener { requireDialog().dismiss() }
        binding.viewStub.setOnClickListener { requireDialog().dismiss() }
        setupFollowing()
//        scrollingOffset = binding.root.height / 4
    }

    private fun initRecycleView(lyrics: AbsLyrics) {
        linearLayoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        lyricsAdapter =
            LyricsAdapter(requireActivity(), lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray(), dialog)
        binding.recyclerViewLyrics
            .apply {
                layoutManager = this@LyricsDialog.linearLayoutManager
                adapter = this@LyricsDialog.lyricsAdapter
            }
    }

    private fun createChip(label: String, index: Int, checked: Boolean = false, callback: (Chip, Int) -> Unit) =
        Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice)
            .apply {
                text = label
                isChecked = checked
                setTextColor(getChipTextColor(checked))
                chipBackgroundColor = getChipBackgroundColor(checked)
                setOnClickListener {
                    callback(it as Chip, index)
                }
            }

    private fun getChipBackgroundColor(checked: Boolean): ColorStateList {
        return ColorStateList.valueOf(
            if (checked) lightenColor(primaryColor)
            else resources.getColor(R.color.defaultFooterColor, requireContext().theme)
        )
    }

    private fun getChipTextColor(checked: Boolean): ColorStateList {
        return ColorStateList.valueOf(
            if (checked) requireContext().secondaryTextColor(primaryColor)
            else textColor
        )
    }

    private var chipSelected: Chip? = null
    private fun initChip() {
        binding.types.isSingleSelection = true
        lyricsInfo.forEachIndexed { index, lyrics ->
            val chip = createChip(
                getLocalizedTypeName(lyrics.source), index, activated == lyrics, this::onChipClicked
            )
            binding.types.addView(chip)
            if (activated == lyrics) chipSelected = chip
        }
        // binding.types.isSelectionRequired = true
    }

    private fun onChipClicked(chip: Chip, index: Int) {
        if (lyricsInfo[index] == activated) return // do not change
        switchLyrics(index)
        chip.isChecked = true
        chip.chipBackgroundColor = getChipBackgroundColor(true)
        chip.setTextColor(getChipTextColor(true))
        chipSelected?.isChecked = false
        chipSelected?.chipBackgroundColor = getChipBackgroundColor(false)
        chipSelected?.setTextColor(getChipTextColor(false))
        chipSelected = chip
    }

    private fun switchLyrics(index: Int) {
        val lyrics = lyricsInfo[index]
        activated = lyrics
        lyricsAdapter.update(lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray())
        val fragment =
            activity?.supportFragmentManager?.findFragmentByTag(AbsSlidingMusicPanelActivity.NOW_PLAYING_FRAGMENT)
        if (fragment != null && fragment is AbsPlayerFragment) {
            fragment.handler.sendMessage(
                Message.obtain(fragment.handler, AbsPlayerFragment.UPDATE_LYRICS).apply {
                    what = AbsPlayerFragment.UPDATE_LYRICS
                    data = Bundle().apply { putParcelable(AbsPlayerFragment.LYRICS, lyrics) }
                }
            )
        }
    }

    private fun getLocalizedTypeName(t: LyricsSource): String =
        when (t.type) {
            LyricsSource.EMBEDDED -> getString(R.string.embedded_lyrics)
            LyricsSource.EXTERNAL_DECORATED, LyricsSource.EXTERNAL_PRECISE -> getString(R.string.external_lyrics)
            LyricsSource.MANUALLY_LOADED -> getString(R.string.loaded)
            else -> "unknown"
        }

    private val accentColor by lazy { ThemeColor.accentColor(App.instance) }
    private val primaryColor by lazy { ThemeColor.primaryColor(App.instance) }
    private val textColor by lazy { App.instance.primaryTextColor(App.instance.nightMode) }

    private val backgroundCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(
                lightenColor(primaryColor),
                lightenColor(primaryColor),
                resources.getColor(R.color.defaultFooterColor, requireContext().theme)
            )
        )
    }
    private val textColorCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(requireContext().primaryTextColor(primaryColor), textColor)
        )
    }

    private fun setupFollowing() {
        binding.lyricsFollowing.apply {
            buttonTintList = backgroundCsl
            setOnCheckedChangeListener { button: CompoundButton, b: Boolean ->
                if (activated is LrcLyrics) {
                    if (_progressViewUpdateHelper == null) {
                        // init
                        _progressViewUpdateHelper = MusicProgressViewUpdateHelper(this@LyricsDialog, 500, 1000)
                    }
                    if (b) progressViewUpdateHelper.start() else progressViewUpdateHelper.stop()
                } else {
                    button.isChecked = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _progressViewUpdateHelper?.destroy()
        _progressViewUpdateHelper = null
    }

    private var _progressViewUpdateHelper: MusicProgressViewUpdateHelper? = null
    private val progressViewUpdateHelper: MusicProgressViewUpdateHelper get() = _progressViewUpdateHelper!!

    private var scrollingOffset: Int = 0
    override fun onUpdateProgressViews(progress: Int, total: Int) {
        if (_viewBinding != null) {
            scrollingTo(progress)
        } else {
            _progressViewUpdateHelper?.destroy()
            _progressViewUpdateHelper = null
        }
    }

    private fun scrollingTo(timeStamp: Int) {
        val lrc = activated as? LrcLyrics
        if (lrc != null) {
            val line = lrc.getPosition(timeStamp)
            linearLayoutManager.smoothScrollToPosition(binding.recyclerViewLyrics, null, line)
//            linearLayoutManager.scrollToPositionWithOffset(line, scrollingOffset)
        }
    }

}
