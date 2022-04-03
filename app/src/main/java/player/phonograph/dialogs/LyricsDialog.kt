package player.phonograph.dialogs

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.iterator
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
import player.phonograph.model.lyrics2.*
import player.phonograph.notification.ErrorNotification
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.MaterialColorHelper
import java.lang.IllegalStateException

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment(), MusicProgressViewUpdateHelper.Callback {

    private var _viewBinding: DialogLyricsBinding? = null
    val binding: DialogLyricsBinding get() = _viewBinding!!

    private lateinit var song: Song
    private lateinit var lyricsPack: LyricsPack
    private lateinit var lyricsDisplay: AbsLyrics
    private var lyricsDisplayType: Int = LyricsPack.UNKNOWN_SOURCE
    private val availableLyricTypes: MutableSet<Int> = HashSet(1)
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().let {
            song = it.getParcelable(SONG)!!
            lyricsPack = it.getParcelable(LYRICS_PACK)!!
            lyricsDisplayType = it.getInt(CURRENT_TYPE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = DialogLyricsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleLyrics()

        binding.title.text = if (lyricsDisplay.getTitle() != DEFAULT_TITLE) lyricsDisplay.getTitle() else song.title
        setupChips()
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

    private fun handleLyrics() {
        if (lyricsPack.external != null) {
            availableLyricTypes.add(LyricsPack.EXTERNAL)
        }
        if (lyricsPack.embedded != null) {
            availableLyricTypes.add(LyricsPack.EMBEDDED)
        }
        if (lyricsPack.externalWithSuffix != null) {
            availableLyricTypes.add(LyricsPack.EXTERNAL_WITH_SUFFIX)
        }
        if (lyricsPack.isEmpty()) {
            availableLyricTypes.add(LyricsPack.UNKNOWN_SOURCE)
        }

        if (lyricsDisplayType == LyricsPack.UNKNOWN_SOURCE) {
            lyricsDisplayType = availableLyricTypes.first() // default
        }
        lyricsDisplay = lyricsPack.getByType(lyricsDisplayType) ?: TextLyrics.from("Empty Lyrics!")
    }

    private fun setupChips() {
        for (type in availableLyricTypes) {
            changeChipVisibility(type, View.VISIBLE)
        }
        binding.types.check(getBindingID(lyricsDisplayType))

        for (chip in binding.types) {
            chip as Chip
            chip.setTextColor(textColorCsl)
            chip.chipBackgroundColor = backgroundCsl
            chip.chipStrokeColor = backgroundCsl
            chip.setOnClickListener {
                val lyrics = when (chip.id) {
                    R.id.chip_embedded_lyrics -> {
                        lyricsDisplayType = LyricsPack.EMBEDDED
                        lyricsPack.embedded!!
                    }
                    R.id.chip_external_lyrics -> {
                        lyricsDisplayType = LyricsPack.EXTERNAL
                        lyricsPack.external!!
                    }
                    R.id.chip_externalWithSuffix_lyrics -> {
                        lyricsDisplayType = LyricsPack.EXTERNAL_WITH_SUFFIX
                        lyricsPack.externalWithSuffix!!
                    }
                    else -> {
                        lyricsDisplayType = LyricsPack.UNKNOWN_SOURCE
                        null
                    }
                }
                if (lyrics != null) {
                    lyricsAdapter.update(lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray())
                    val fragment = activity?.supportFragmentManager?.findFragmentByTag(AbsSlidingMusicPanelActivity.NOW_PLAYING_FRAGMENT)
                    if (fragment != null && fragment is AbsPlayerFragment) {
                        fragment.handler.sendMessage(
                            Message.obtain(fragment.handler, AbsPlayerFragment.UPDATE_LYRICS).apply {
                                what = AbsPlayerFragment.UPDATE_LYRICS
                                data = Bundle().apply {
                                    putParcelable(AbsPlayerFragment.LYRICS, lyrics)
                                    putInt(AbsPlayerFragment.LYRICS_SOURCE, lyricsDisplayType)
                                }
                            }
                        )
                    }
                }
            }
        }
        binding.types.isSelectionRequired = true
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
            setOnCheckedChangeListener { _: CompoundButton, b: Boolean ->
                if (b) progressViewUpdateHelper.start() else progressViewUpdateHelper.stop()
            }
        }
    }

    private fun changeChipVisibility(type: Int, visibility: Int) {
        when (type) {
            LyricsPack.EMBEDDED -> {
                binding.chipEmbeddedLyrics.visibility = visibility
            }
            LyricsPack.EXTERNAL -> {
                binding.chipExternalLyrics.visibility = visibility
            }
            LyricsPack.EXTERNAL_WITH_SUFFIX -> {
                binding.chipExternalWithSuffixLyrics.visibility = visibility
            }
        }
    }
    private fun getBindingID(lyricsDisplayType: Int): Int =
        when (lyricsDisplayType) {
            LyricsPack.EMBEDDED -> {
                binding.chipEmbeddedLyrics.id
            }
            LyricsPack.EXTERNAL -> {
                binding.chipExternalLyrics.id
            }
            LyricsPack.EXTERNAL_WITH_SUFFIX -> {
                binding.chipExternalWithSuffixLyrics.id
            }
            else -> {
                ErrorNotification.postErrorNotification(
                    IllegalStateException("Unknown lyricsDisplayType($lyricsDisplayType)").apply { stackTrace = Thread.currentThread().stackTrace }, null
                )
                binding.chipEmbeddedLyrics.id
            }
        }

    private fun setCheckStatus(type: Int, state: Boolean) {
        when (type) {
            LyricsPack.EMBEDDED -> {
                binding.chipEmbeddedLyrics.isChecked = state
            }
            LyricsPack.EXTERNAL -> {
                binding.chipExternalLyrics.isChecked = state
            }
            LyricsPack.EXTERNAL_WITH_SUFFIX -> {
                binding.chipExternalWithSuffixLyrics.isChecked = state
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

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }

    private val progressViewUpdateHelper: MusicProgressViewUpdateHelper by lazy(LazyThreadSafetyMode.NONE) {
        MusicProgressViewUpdateHelper(this, 500, 1000)
    }

    private var scrollingOffset: Int = 0
    override fun onUpdateProgressViews(progress: Int, total: Int) {
        if (this.isHidden || this.isDetached) {
            progressViewUpdateHelper.stop()
        } else {
            scrollingTo(progress)
        }
    }
    private fun scrollingTo(timeStamp: Int) {
        if (lyricsDisplay is LrcLyrics) {
            val line = (lyricsDisplay as LrcLyrics).getPosition(timeStamp)
            linearLayoutManager.scrollToPositionWithOffset(line, scrollingOffset)
        }
    }

    companion object {
        private const val SONG = "song"
        private const val LYRICS_PACK = "lyrics_pack"
        private const val CURRENT_TYPE = "current_type"

        fun create(lyricsPack: LyricsPack, song: Song, currentType: Int = LyricsPack.UNKNOWN_SOURCE): LyricsDialog =
            LyricsDialog()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG, song)
                        putParcelable(LYRICS_PACK, lyricsPack)
                        putInt(CURRENT_TYPE, currentType)
                    }
                }
    }
}
