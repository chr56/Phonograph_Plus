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
    private lateinit var lyrics: AbsLyrics
    private lateinit var lyricsPack: LyricsPack

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        requireArguments().let {
            song = it.getParcelable(SONG)!!
            val p = it.getParcelable<LyricsPack>(LYRICS_PACK)
            if (p != null) {
                lyricsPack = p
                lyrics = getLyrics(p)!!
            } else {
                lyrics = it.getParcelable(LYRICS)!!
            }
        }
        val title: String = if (lyrics.getTitle() != DEFAULT_TITLE) lyrics.getTitle() else song.title

        val dialog = MaterialDialog(requireActivity())
            .title(text = title)
            .positiveButton { dismiss() }
            .customView(R.layout.dialog_lyrics, horizontalPadding = true)

        dialog.getCustomView()
            .findViewById<RecyclerView>(R.id.recycler_view_lyrics)
            .apply {
                layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
                adapter = LyricsAdapter(requireActivity(), lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray(), dialog)
            }

        return dialog
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

        fun create(lyrics: AbsLyrics, song: Song): LyricsDialog =
            LyricsDialog()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG, song)
                        putParcelable(LYRICS, lyrics)
                    }
                }
    }
}
