package player.phonograph.dialogs

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.iterator
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.LyricsAdapter
import player.phonograph.databinding.DialogLyricsBinding
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.AbsLyrics
import player.phonograph.model.lyrics2.DEFAULT_TITLE
import player.phonograph.model.lyrics2.LyricsPack
import player.phonograph.model.lyrics2.TextLyrics
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment() {

    private var _viewBinding: DialogLyricsBinding? = null
    val binding: DialogLyricsBinding get() = _viewBinding!!

    private lateinit var song: Song
    private lateinit var lyricsPack: LyricsPack
    private lateinit var lyricsDisplay: AbsLyrics
    private var lyricsDisplayType: Int = LyricsPack.NO_LYRICS
    private val availableLyricTypes: MutableSet<Int> = HashSet(1)
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().let {
            song = it.getParcelable(SONG)!!
            lyricsPack = it.getParcelable(LYRICS_PACK)!!
//            if (lyricsPack.isEmpty())
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
        if (lyricsPack.external != null) { availableLyricTypes.add(LyricsPack.EXTERNAL) }
        if (lyricsPack.embedded != null) { availableLyricTypes.add(LyricsPack.EMBEDDED) }
        if (lyricsPack.externalWithSuffix != null) { availableLyricTypes.add(LyricsPack.EXTERNAL_WITH_SUFFIX) }
        if (lyricsPack.isEmpty()) { availableLyricTypes.add(LyricsPack.NO_LYRICS) }

        lyricsDisplayType = availableLyricTypes.first()
        lyricsDisplay = lyricsPack.getByType(availableLyricTypes.first()) ?: TextLyrics.from("Empty Lyrics!")
    }

    private fun setupChips() {
        for (type in availableLyricTypes) {
            changeChipVisibility(type, View.VISIBLE)
        }
        setCheckStatus(lyricsDisplayType, true)

        for (chip in binding.types) {
            chip.setOnClickListener {
                (chip as Chip).isChecked = true
                when (chip.id) {
                    R.id.chip_embedded_lyrics -> {
                        lyricsPack.embedded!!.let { lyricsAdapter.update(it.getLyricsTimeArray(), it.getLyricsLineArray()) }
                    }
                    R.id.chip_external_lyrics -> {
                        lyricsPack.external!!.let { lyricsAdapter.update(it.getLyricsTimeArray(), it.getLyricsLineArray()) }
                    }
                    R.id.chip_externalWithSuffix_lyrics -> {
                        lyricsPack.externalWithSuffix!!.let { lyricsAdapter.update(it.getLyricsTimeArray(), it.getLyricsLineArray()) }
                    }
                }
            }
        }

        val primaryColorTransparent = ColorUtil.withAlpha(primaryColor, 0.75f)
        binding.chipEmbeddedLyrics.chipStrokeColor = ColorStateList.valueOf(primaryColorTransparent)
        binding.chipExternalLyrics.chipStrokeColor = ColorStateList.valueOf(primaryColorTransparent)
        binding.chipExternalWithSuffixLyrics.chipStrokeColor = ColorStateList.valueOf(primaryColorTransparent)

        binding.chipEmbeddedLyrics.chipBackgroundColor = backgroundCsl
        binding.chipExternalLyrics.chipBackgroundColor = backgroundCsl
        binding.chipExternalWithSuffixLyrics.chipBackgroundColor = backgroundCsl
    }

    private val accentColor by lazy { ThemeColor.accentColor(App.instance) }
    private val primaryColor by lazy { ThemeColor.primaryColor(App.instance) }
    private val textColor by lazy { ThemeColor.textColorSecondary(App.instance) }

    private val backgroundCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf()
            ),
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT)
        )
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

    companion object {
        private const val SONG = "song"
        private const val LYRICS_PACK = "lyrics_pack"

        fun create(lyricsPack: LyricsPack, song: Song): LyricsDialog =
            LyricsDialog()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG, song)
                        putParcelable(LYRICS_PACK, lyricsPack)
                    }
                }
    }
}
