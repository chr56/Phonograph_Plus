package player.phonograph.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import player.phonograph.R
import player.phonograph.adapter.LyricsAdapter
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment() {
    private lateinit var song: Song
    private lateinit var lyricsPack: LyricsPack

    private lateinit var adapter: LyricsAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        requireArguments().let {
            song = it.getParcelable(SONG)!!
            lyricsPack = it.getParcelable(LYRICS_PACK)!!
            if (lyricsPack.isEmpty()) return MaterialDialog(requireActivity()).message(text = getString(R.string.empty))
        }

        val lyrics = lyricsPack.getLyrics()!!

        val title: String = if (lyrics.getTitle() != DEFAULT_TITLE) lyrics.getTitle() else song.title

        val dialog = MaterialDialog(requireActivity())
            .title(text = title)
            .positiveButton { dismiss() }
            .customView(R.layout.dialog_lyrics, horizontalPadding = true)

        setUpRecycleView(
            lyrics, dialog.getCustomView().findViewById(R.id.recycler_view_lyrics)
        )

        return dialog
    }

    private fun setUpRecycleView(lyrics: AbsLyrics, recyclerView: RecyclerView) {
        layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        adapter = LyricsAdapter(requireActivity(), lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray(), dialog)
        recyclerView
            .apply {
                layoutManager = this.layoutManager
                adapter = this.adapter
            }
    }

    companion object {
        private const val SONG = "song"
        private const val LYRICS = "lyrics"
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
