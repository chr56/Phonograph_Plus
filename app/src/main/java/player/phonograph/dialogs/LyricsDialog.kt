package player.phonograph.dialogs

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.LyricsAdapter
import player.phonograph.databinding.DialogLyricsBinding
import player.phonograph.helper.MusicProgressViewUpdateHelper
import player.phonograph.model.Song
import player.phonograph.model.lyrics.*
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.MaterialColorHelper

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment(), MusicProgressViewUpdateHelper.Callback {

    private var _viewBinding: DialogLyricsBinding? = null
    val binding: DialogLyricsBinding get() = _viewBinding!!

    private lateinit var song: Song
    private lateinit var lyricsList: LyricsList
    private lateinit var lyricsDisplay: AbsLyrics
    private val availableLyricTypes: MutableSet<LyricsSource> = HashSet(1)
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().let {
            song = it.getParcelable(SONG)!!
            lyricsList = it.getParcelable(LYRICS_PACK)!!
            lyricsDisplay = it.getParcelable(CURRENT_LYRICS)!!
        }
        if (lyricsList.list.isEmpty()) {
            throw IllegalStateException("No lyrics?!")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = DialogLyricsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        availableLyricTypes.addAll(lyricsList.getAvailableTypes().orEmpty())

        binding.title.text = if (lyricsDisplay.getTitle() != DEFAULT_TITLE) lyricsDisplay.getTitle() else song.title
        initChip()
        initRecycleView(lyricsDisplay)

        // corner
        requireDialog().window!!.setBackgroundDrawable(
            GradientDrawable().apply {
                this.cornerRadius = 0f
                setColor(requireContext().theme.obtainStyledAttributes(intArrayOf(R.attr.colorBackgroundFloating)).getColor(0, 0))
            }
        )
        binding.ok.setOnClickListener { requireDialog().dismiss() }
        binding.viewStub.setOnClickListener { requireDialog().dismiss() }
        setupFollowing()
//        scrollingOffset = binding.root.height / 4
    }

    private fun initRecycleView(lyrics: AbsLyrics) {
        linearLayoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        lyricsAdapter = LyricsAdapter(requireActivity(), lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray(), dialog)
        binding.recyclerViewLyrics
            .apply {
                layoutManager = this@LyricsDialog.linearLayoutManager
                adapter = this@LyricsDialog.lyricsAdapter
            }
    }

    private fun createChip(text: String, index: Int, checked: Boolean = false, callback: (Chip, Int) -> Unit): Chip {
        val chip = Chip(requireContext(), null, R.style.Widget_MaterialComponents_Chip_Choice)
        chip.text = text
        chip.isChecked = checked
        chip.setTextColor(getChipTextColor(checked))
        chip.chipBackgroundColor = getChipBackgroundColor(checked)
        chip.setOnClickListener {
            callback(it as Chip, index)
        }
        return chip
    }
    private fun getChipBackgroundColor(checked: Boolean): ColorStateList {
        return ColorStateList.valueOf(
            if (checked) ColorUtil.lightenColor(primaryColor)
            else resources.getColor(R.color.defaultFooterColor, requireContext().theme)
        )
    }
    private fun getChipTextColor(checked: Boolean): ColorStateList {
        return ColorStateList.valueOf(
            if (checked) MaterialColorHelper.getPrimaryTextColor(requireContext(), ColorUtil.isColorLight(primaryColor))
            else textColor
        )
    }

    private var chipSelected: Chip? = null
    private fun initChip() {
        binding.types.isSingleSelection = true
        lyricsList.list.forEachIndexed { index, lyrics ->
            val chip = createChip(
                getLocalizedTypeName(lyrics.source), index, lyricsDisplay == lyrics, this::onChipClicked
            )
            binding.types.addView(chip)
            if (lyricsDisplay == lyrics) chipSelected = chip
        }
        // binding.types.isSelectionRequired = true
    }

    private fun onChipClicked(chip: Chip, index: Int) {
        if (lyricsList.list[index] == lyricsDisplay) return // do not change
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
        val lyrics = lyricsList.list[index]
        lyricsDisplay = lyrics
        lyricsAdapter.update(lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray())
        val fragment = activity?.supportFragmentManager?.findFragmentByTag(AbsSlidingMusicPanelActivity.NOW_PLAYING_FRAGMENT)
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
            else -> "unknown"
        }

    private val accentColor by lazy { ThemeColor.accentColor(App.instance) }
    private val primaryColor by lazy { ThemeColor.primaryColor(App.instance) }
    private val textColor by lazy { ThemeColor.textColorSecondary(App.instance) }

    private val backgroundCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(ColorUtil.lightenColor(primaryColor), ColorUtil.lightenColor(primaryColor), resources.getColor(R.color.defaultFooterColor, requireContext().theme))
        )
    }
    private val textColorCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(MaterialColorHelper.getPrimaryTextColor(requireContext(), ColorUtil.isColorLight(primaryColor)), textColor)
        )
    }

    private fun setupFollowing() {
        binding.lyricsFollowing.apply {
            buttonTintList = backgroundCsl
            setOnCheckedChangeListener { button: CompoundButton, b: Boolean ->
                if (lyricsDisplay is LrcLyrics) {
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

    override fun onStart() {
        // set up size
        requireDialog().window!!.attributes =
            requireDialog().window!!.let { window ->
                window.attributes.apply {
                    width = (requireActivity().window.decorView.width * 0.90).toInt()
                    height = (requireActivity().window.decorView.height * 0.90).toInt()
                }
            }

        super.onStart()
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
        if (lyricsDisplay is LrcLyrics) {
            val line = (lyricsDisplay as LrcLyrics).getPosition(timeStamp)
            linearLayoutManager.smoothScrollToPosition(binding.recyclerViewLyrics, null, line)
//            linearLayoutManager.scrollToPositionWithOffset(line, scrollingOffset)
        }
    }

    companion object {
        private const val SONG = "song"
        private const val LYRICS_PACK = "lyrics_pack"
        private const val CURRENT_LYRICS = "current_lyrics"

        fun create(lyricsList: LyricsList, song: Song, currentLyrics: AbsLyrics): LyricsDialog =
            LyricsDialog()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG, song)
                        putParcelable(LYRICS_PACK, lyricsList)
                        putParcelable(CURRENT_LYRICS, currentLyrics)
                    }
                }
    }
}
