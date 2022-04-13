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
    private lateinit var lyricsSet: LyricsSet
    private lateinit var lyricsDisplay: Lyrics
    private var lyricsDisplayType: LyricsSource = LyricsSource.Unknown()
    private val availableLyricTypes: MutableSet<LyricsSource> = HashSet(1)
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().let {
            song = it.getParcelable(SONG)!!
            lyricsSet = it.getParcelable(LYRICS_PACK)!!
            lyricsDisplayType = LyricsSource(it.getInt(CURRENT_TYPE))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = DialogLyricsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleLyrics()

        binding.title.text = if (lyricsDisplay.content.getTitle() != DEFAULT_TITLE) lyricsDisplay.content.getTitle() else song.title
        setupChips()
        initRecycleView(lyricsDisplay.content)

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
        if (!lyricsSet.external.isNullOrEmpty()) {
            availableLyricTypes.add(LyricsSource.ExternalPrecise())
        }
        if (lyricsSet.embedded != null) {
            availableLyricTypes.add(LyricsSource.Embedded())
        }
        if (lyricsSet.isEmpty()) {
            availableLyricTypes.add(LyricsSource.Unknown())
        }

        if (lyricsDisplayType == LyricsSource.Unknown()) {
            lyricsDisplayType = availableLyricTypes.first() // default
        }
        lyricsDisplay = Lyrics(lyricsSet.getByType(lyricsDisplayType) ?: TextLyrics.from("Empty Lyrics!"), lyricsDisplayType)
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
                val lyrics: AbsLyrics? = when (chip.id) {
                    R.id.chip_embedded_lyrics -> {
                        lyricsDisplayType = LyricsSource.Embedded()
                        lyricsSet.embedded!!.content
                    }
                    R.id.chip_external_lyrics -> {
                        lyricsDisplayType = LyricsSource.ExternalPrecise()
                        lyricsSet.external!!.let {
                            if (it.isNotEmpty()) {
                                var ret: AbsLyrics? = null
                                for (l in it) { if (l.source.type == LyricsSource.EXTERNAL_PRECISE) ret = l.content }
                                ret
                            } else null
                        }
                    }
                    R.id.chip_externalWithSuffix_lyrics -> {
                        lyricsDisplayType = LyricsSource.ExternalDecorated()
                        lyricsSet.external!!.let {
                            if (it.isNotEmpty()) {
                                var ret: AbsLyrics? = null
                                for (l in it) { if (l.source.type == LyricsSource.EXTERNAL_DECORATED) ret = l.content }
                                ret
                            } else null
                        }
                    }
                    else -> {
                        lyricsDisplayType = LyricsSource.Unknown()
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
                                    putParcelable(AbsPlayerFragment.LYRICS, Lyrics(lyrics, lyricsDisplayType))
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
            setOnCheckedChangeListener { button: CompoundButton, b: Boolean ->
                if (lyricsDisplay.content is LrcLyrics) {
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

    private fun changeChipVisibility(type: LyricsSource, visibility: Int) {
        when (type) {
            LyricsSource.Embedded() -> {
                binding.chipEmbeddedLyrics.visibility = visibility
            }
            LyricsSource.ExternalPrecise() -> {
                binding.chipExternalLyrics.visibility = visibility
            }
            LyricsSource.ExternalDecorated() -> {
                binding.chipExternalWithSuffixLyrics.visibility = visibility
            }
        }
    }
    private fun getBindingID(lyricsDisplayType: LyricsSource): Int =
        when (lyricsDisplayType.type) {
            LyricsSource.EMBEDDED -> {
                binding.chipEmbeddedLyrics.id
            }
            LyricsSource.EXTERNAL_PRECISE -> {
                binding.chipExternalLyrics.id
            }
            LyricsSource.EXTERNAL_DECORATED -> {
                binding.chipExternalWithSuffixLyrics.id
            }
            else -> {
                ErrorNotification.postErrorNotification(
                    IllegalStateException("Unknown lyricsDisplayType($lyricsDisplayType)").apply { stackTrace = Thread.currentThread().stackTrace }, null
                )
                binding.chipEmbeddedLyrics.id
            }
        }

    private fun setCheckStatus(type: LyricsSource, state: Boolean) {
        when (type) {
            LyricsSource.Embedded() -> {
                binding.chipEmbeddedLyrics.isChecked = state
            }
            LyricsSource.Embedded() -> {
                binding.chipExternalLyrics.isChecked = state
            }
            LyricsSource.ExternalDecorated() -> {
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
        if (lyricsDisplay.content is LrcLyrics) {
            val line = (lyricsDisplay.content as LrcLyrics).getPosition(timeStamp)
            linearLayoutManager.smoothScrollToPosition(binding.recyclerViewLyrics, null, line)
//            linearLayoutManager.scrollToPositionWithOffset(line, scrollingOffset)
        }
    }

    companion object {
        private const val SONG = "song"
        private const val LYRICS_PACK = "lyrics_pack"
        private const val CURRENT_TYPE = "current_type"

        fun create(lyricsSet: LyricsSet, song: Song, currentType: Int = LyricsSource.UNKNOWN_SOURCE): LyricsDialog =
            LyricsDialog()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG, song)
                        putParcelable(LYRICS_PACK, lyricsSet)
                        putInt(CURRENT_TYPE, currentType)
                    }
                }
    }
}
