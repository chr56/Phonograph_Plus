package player.phonograph.dialogs

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        super.onViewCreated(view, savedInstanceState)

        val lyrics = lyricsPack.getLyrics()?:TextLyrics.from("Empty Lyrics!")
        val title: String = if (lyrics.getTitle() != DEFAULT_TITLE) lyrics.getTitle() else song.title
        binding.title.text = title

        setUpRecycleView(lyrics)

        initChip()

        binding.ok.setOnClickListener { requireDialog().dismiss() }
        binding.viewStub.setOnClickListener { requireDialog().dismiss() }

        requireDialog().window!!.setBackgroundDrawable(
            GradientDrawable().apply {
                this.cornerRadius = 0f
                setColor(requireContext().theme.obtainStyledAttributes(intArrayOf(R.attr.colorBackgroundFloating)).getColor(0, 0))
            }
        )
    }

    private fun setUpRecycleView(lyrics: AbsLyrics) {
        linearLayoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        lyricsAdapter = LyricsAdapter(requireActivity(), lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray(), dialog)
        binding.recyclerViewLyrics
            .apply {
                layoutManager = this@LyricsDialog.linearLayoutManager
                adapter = this@LyricsDialog.lyricsAdapter
            }
    }

    private fun initChip() {
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
