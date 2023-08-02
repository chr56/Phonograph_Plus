package player.phonograph.adapter

import player.phonograph.R
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.util.text.parseTimeStamp
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.regex.Pattern

class LyricsAdapter(
    private val context: Context,
    stamps: IntArray,
    lines: Array<String>,
    private val dismiss: (() -> Unit)?,
) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    private var lyrics = lines
    private var timeStamps = stamps

    @SuppressLint("NotifyDataSetChanged")
    fun update(stamps: IntArray, lines: Array<String>) {
        lyrics = lines
        timeStamps = stamps
        notifyDataSetChanged()
    }

    class ViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val line: TextView = itemView.findViewById(R.id.dialog_lyrics_line)
        val time: TextView = itemView.findViewById(R.id.dialog_lyrics_times)

        fun bind(context: Context, lyrics: Array<String>, timeStamps: IntArray, dismiss: (() -> Unit)?) {
            // parse line feed
            val actual = StringBuffer()
            lyrics[bindingAdapterPosition].split(Pattern.compile("\\\\[nNrR]")).forEach {
                actual.append(it).appendLine()
            }

            time.text = parseTimeStamp(timeStamps[bindingAdapterPosition])
            time.setTextColor(context.getColor(R.color.dividerColor))
            if (timeStamps[bindingAdapterPosition] < 0 || !Setting.instance.displaySynchronizedLyricsTimeAxis)
                time.visibility = View.GONE

            line.text = actual.trim().toString()

            line.setOnLongClickListener {
                MusicPlayerRemote.seekTo(timeStamps[bindingAdapterPosition])
                dismiss?.invoke()
                true
            }
            line.typeface = Typeface.DEFAULT
            time.typeface = Typeface.DEFAULT
        }

        fun highlight(highlight: Boolean) {
            line.typeface = if (highlight) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            time.typeface = if (highlight) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        companion object {
            fun inflate(context: Context, parent: ViewGroup) =
                ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_lyrics, parent, false))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder.inflate(context, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(context, lyrics, timeStamps, dismiss)
    }

    override fun getItemCount(): Int = lyrics.size

}