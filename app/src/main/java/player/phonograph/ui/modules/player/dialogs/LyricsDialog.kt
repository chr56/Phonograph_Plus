/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.dialogs

import com.google.android.material.chip.Chip
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import player.phonograph.R
import player.phonograph.databinding.DialogLyricsBinding
import player.phonograph.foundation.error.warning
import player.phonograph.foundation.lyricsTimestamp
import player.phonograph.mechanism.lyrics.ActualTextLyrics
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LrcWord
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.model.lyrics.TextLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.modules.player.LyricsViewModel
import player.phonograph.ui.modules.player.MusicProgressUpdateDelegate
import player.phonograph.ui.resource.Texts
import player.phonograph.ui.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.textColorOn
import player.phonograph.ui.theme.themeFooterColor
import player.phonograph.ui.util.applyLargeDialog
import player.phonograph.ui.util.observe
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Large Dialog to show Lyrics.
 *
 * **MUST** be created from a view-model owner possessing [LyricsViewModel]
 */
class LyricsDialog : DialogFragment() {

    private var _viewBinding: DialogLyricsBinding? = null
    val binding: DialogLyricsBinding get() = _viewBinding!!

    private val viewModel: LyricsViewModel by viewModels({ requireActivity() })

    //region Fragment LifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(progressUpdateDelegate)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = DialogLyricsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.ok.setOnClickListener { requireDialog().dismiss() }
        binding.viewStub.setOnClickListener { requireDialog().dismiss() }

        scroller = LyricsSmoothScroller(view.context)

        val lyricsInfo: LyricsInfo? = viewModel.lyricsInfo.value
        if (lyricsInfo == null) {
            dismissNow()
            return
        }

        updateChips(lyricsInfo)
        updateTitle(lyricsInfo)
        setupRecycleView(lyricsInfo)

        setupFollowing(lyricsInfo)

        observe(viewLifecycleOwner.lifecycle, viewModel.lyricsInfo) { info ->
            withContext(Dispatchers.Main) {
                updateTitle(info)
                updateChips(info)
                updateRecycleView(info)
                lyricsAdapter.clearHighlight()
            }
        }
        observe(viewLifecycleOwner.lifecycle, viewModel.requireLyricsFollowing) { following ->
            binding.lyricsFollowing.isChecked = following
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onStart() {
        super.onStart()
        applyLargeDialog()
    }

    //endregion


    //region Chip & Title


    private var chipSelected: Chip? = null
    private fun updateChips(info: LyricsInfo?) {
        binding.types.removeAllViews()
        binding.types.isSingleSelection = true
        if (info == null) return
        for ((index, lyrics) in info.withIndex()) {
            val requireCheck = info.isActive(index)
            val chip = createChip(
                Texts.lyricsSource(resources, lyrics.source), index, requireCheck, 0, this::onChipClicked
            )
            binding.types.addView(chip)
            if (requireCheck) chipSelected = chip
        }
        binding.types.addView(
            createChip(
                getString(R.string.action_load),
                -1,
                false,
                R.drawable.ic_add_white_24dp
            ) { _, _ -> manualLoadLyrics() })
        // binding.types.isSelectionRequired = true
    }


    private fun createChip(
        label: String,
        index: Int,
        checked: Boolean = false,
        iconRes: Int = 0,
        callback: (Chip, Int) -> Unit,
    ) = Chip(requireContext()).apply {
        text = label
        isChecked = checked
        setTextColor(chipTextColor(checked))
        chipBackgroundColor = chipBackgroundColor(checked)
        setOnClickListener {
            callback(it as Chip, index)
        }
        if (iconRes != 0) {
            chipIcon = getTintedDrawable(
                iconRes, textColorOn(
                    requireContext(), if (checked) primaryColor()
                    else themeFooterColor(requireContext())
                )
            )
        }
    }

    private fun onChipClicked(chip: Chip, index: Int) {

        chipSelected?.chipBackgroundColor = chipBackgroundColor(false)
        chipSelected?.setTextColor(chipTextColor(false))
        chipSelected = chip
        chip.chipBackgroundColor = chipBackgroundColor(true)
        chip.setTextColor(chipTextColor(true))

        val lyricsInfo = viewModel.lyricsInfo.value ?: return
        lifecycleScope.launch {
            viewModel.activateLyrics(lyricsInfo[index])
        }
    }

    private fun updateTitle(info: LyricsInfo?) {
        val activated = info?.activatedLyrics
        binding.title.text = activated?.title ?: AbsLyrics.DEFAULT_TITLE
    }

    //endregion


    //region Manual Load
    private fun manualLoadLyrics() {
        val activity = requireActivity()
        val accessor = activity as? IOpenFileStorageAccessible
        if (accessor != null) {
            accessor.openFileStorageAccessDelegate.launch(OpenDocumentContract.Config(arrayOf("*/*"))) { uri ->
                if (uri == null) return@launch
                CoroutineScope(Dispatchers.IO).launch {
                    val lyricsViewModel = ViewModelProvider(activity)[LyricsViewModel::class.java]
                    lyricsViewModel.appendLyricsFrom(activity, uri)
                }
            }
        } else {
            warning(activity, "Lyrics", "Can not open file from $activity")
        }
    }
    //endregion

    //region RecycleView
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private fun setupRecycleView(lyricsInfo: LyricsInfo) {
        val context = requireContext()
        val lyrics =
            lyricsInfo.activatedLyrics ?: lyricsInfo.getOrElse(0) { ActualTextLyrics.from("NOT FOUND!?") }
        linearLayoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        lyricsAdapter = LyricsAdapter(
            context,
            lyrics,
            settings = LyricsAdapter.DisplaySettings.read(context),
            highlightColor = primaryColor(),
        ) { dialog?.dismiss() }
        binding.recyclerViewLyrics.apply {
            layoutManager = this@LyricsDialog.linearLayoutManager
            adapter = this@LyricsDialog.lyricsAdapter
        }
    }

    private fun updateRecycleView(info: LyricsInfo?) {
        val activated = info?.activatedLyrics
        if (activated != null) {
            binding.recyclerViewLyrics.visibility = View.VISIBLE
            lyricsAdapter.update(activated)
        } else {
            binding.recyclerViewLyrics.visibility = View.INVISIBLE
        }
    }
    //endregion


    //region Scroll

    private fun setupFollowing(info: LyricsInfo?) {
        binding.lyricsFollowing.apply {
            buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                    intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected),
                    intArrayOf(android.R.attr.state_enabled),
                    intArrayOf(),
                ), intArrayOf(
                    primaryColor(),
                    primaryColor(),
                    themeFooterColor(requireContext()),
                    themeFooterColor(requireContext()),
                )
            )
            setOnCheckedChangeListener { button: CompoundButton, newValue: Boolean ->
                viewModel.updateRequireLyricsFollowing(
                    if (info?.activatedLyrics is LrcLyrics) {
                        newValue
                    } else {
                        // text lyrics can not follow
                        button.isChecked = false
                        false
                    }
                )
            }
        }
    }


    private val progressUpdateDelegate = MusicProgressUpdateDelegate(::onUpdateProgress, 500, 1000)
    private fun onUpdateProgress(progress: Int, total: Int) {
        val lyrics = viewModel.lyricsInfo.value?.activatedLyrics
        val lrcLyrics = lyrics as? LrcLyrics ?: return
        val position = lrcLyrics.getLineNumber(progress)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lyricsAdapter.updateHighlight(position, progress)
            if (viewModel.requireLyricsFollowing.value) {
                scrollingTo(position)
            }
        }
    }

    private lateinit var scroller: LyricsSmoothScroller

    class LyricsSmoothScroller(context: Context) : LinearSmoothScroller(context) {

        private val densityDpi = context.resources.displayMetrics.densityDpi
        private val minDeviation = densityDpi / 10
        private val offset = densityDpi / 5

        override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
            val dyStart = calculateDyToMakeVisible(targetView, SNAP_TO_START)
            val dyEnd = calculateDyToMakeVisible(targetView, SNAP_TO_END)
            var dy = (dyEnd + dyStart) / 2
            if (abs(dy) < minDeviation) dy = 0  // omit slight deviation
            dy -= offset // slightly upper
            // debug { Log.v("SmoothScroller", "dy:$dy dyStart:$dyStart dyEnd:$dyEnd") }
            val time = calculateTimeForDeceleration(dy)
            if (time > 0) {
                action.update(0, -dy, time, mDecelerateInterpolator)
            }
        }
    }

    private fun scrollingTo(position: Int) {
        try {
            if (position >= 0) {
                scroller.targetPosition = position
                linearLayoutManager.startSmoothScroll(scroller)
            }
        } catch (e: Exception) {
            warning(requireContext(), "LyricsScroll", "Failed to scroll to $position", e)
        }
    }

    //endregion


    //region Theme & Color

    private fun chipBackgroundColor(checked: Boolean) = ColorStateList.valueOf(
        if (checked) primaryColor() else themeFooterColor(requireContext())
    )

    private fun chipTextColor(checked: Boolean) = ColorStateList.valueOf(
        if (checked) textColorOn(requireContext(), primaryColor())
        else textColorOn(requireContext(), themeFooterColor(requireContext()))
    )

    //endregion

}

private class LyricsAdapter(
    private val context: Context,
    private var lyric: AbsLyrics,
    private var settings: DisplaySettings,
    private val highlightColor: Int,
    private val dismiss: (() -> Unit)?,
) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    private var lyricLines: Array<String> = lyric.lyricsLineArray
    private var lyricTimestamps: IntArray = lyric.lyricsTimeArray
    private var highlightedPosition: Int = -1
    private var highlightedWordIndex: Int = -1

    @SuppressLint("NotifyDataSetChanged")
    fun update(newLyric: AbsLyrics) {
        lyric = newLyric
        lyricLines = newLyric.lyricsLineArray
        lyricTimestamps = newLyric.lyricsTimeArray
        highlightedPosition = -1
        highlightedWordIndex = -1
        notifyDataSetChanged()
    }

    fun updateHighlight(position: Int, progress: Int) {
        val lrcLyrics = lyric as? LrcLyrics
        val wordIndex = if (position >= 0 && lrcLyrics != null) {
            lrcLyrics.getWordIndex(position, progress)
        } else {
            -1
        }
        if (position == highlightedPosition && wordIndex == highlightedWordIndex) return

        val previousPosition = highlightedPosition
        highlightedPosition = position
        highlightedWordIndex = wordIndex

        if (previousPosition >= 0 && previousPosition != position) notifyItemChanged(previousPosition)
        if (position >= 0) notifyItemChanged(position)
    }

    fun clearHighlight() {
        val previousPosition = highlightedPosition
        highlightedPosition = -1
        highlightedWordIndex = -1
        if (previousPosition >= 0) notifyItemChanged(previousPosition)
    }

    class ViewHolder private constructor(itemView: View, val settings: DisplaySettings) :
            RecyclerView.ViewHolder(itemView) {

        val textLine: TextView = itemView.findViewById(R.id.dialog_lyrics_line)
        val textTime: TextView = itemView.findViewById(R.id.dialog_lyrics_times)

        fun bind(
            line: String,
            timestamp: Int,
            dismiss: (() -> Unit)?,
            showTimestamp: Boolean = settings.enableTimestamp,
            highlighted: Boolean = false,
            words: List<LrcWord> = emptyList(),
            highlightedWordIndex: Int = -1,
            highlightColor: Int = 0,
        ) {
            // Text Line
            textLine.text = if (highlighted && words.isNotEmpty() && highlightedWordIndex >= 0) {
                buildHighlightedLine(words, highlightedWordIndex, highlightColor)
            } else {
                formatLineFeed(line).trim()
            }
            textLine.typeface = if (highlighted) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textLine.textSize = settings.fontSize
            textLine.setOnLongClickListener {
                if (timestamp >= 0) {
                    MusicPlayerRemote.seekTo(timestamp)
                    dismiss?.invoke()
                }
                true
            }

            // Text Timestamp
            if (showTimestamp) {
                textTime.text = lyricsTimestamp(timestamp)
                textTime.typeface = if (highlighted) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                textTime.textSize = max(settings.fontSize - 4, 6f)
                textTime.visibility = View.VISIBLE
            } else {
                textTime.visibility = View.GONE
            }
        }

        private fun formatLineFeed(line: String): String =
            line.replace(LINE_FEED_PATTERN, "\n")

        private fun buildHighlightedLine(
            words: List<LrcWord>,
            highlightedWordIndex: Int,
            highlightColor: Int,
        ): CharSequence {
            val builder = SpannableStringBuilder()
            words.forEachIndexed { index, word ->
                val start = builder.length
                builder.append(formatLineFeed(word.text))
                if (index <= highlightedWordIndex && builder.length > start) {
                    builder.setSpan(
                        ForegroundColorSpan(highlightColor),
                        start,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            var start = 0
            while (start < builder.length && builder[start].isWhitespace()) start++
            var end = builder.length
            while (end > start && builder[end - 1].isWhitespace()) end--
            if (end < builder.length) builder.delete(end, builder.length)
            if (start > 0) builder.delete(0, start)
            return builder
        }

        companion object {
            private val LINE_FEED_PATTERN = Regex("""\\[nNrR]""")

            fun inflate(context: Context, parent: ViewGroup, settings: DisplaySettings) =
                ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_lyrics, parent, false), settings)
        }
    }

    class DisplaySettings(
        val enableTimestamp: Boolean,
        val fontSize: Float,
    ) {
        companion object {
            fun read(context: Context): DisplaySettings {
                val enableTimestamp = Settings(context)[Keys.displaySynchronizedLyricsTimeAxis].data
                val fontSize = Settings(context)[Keys.dialogLyricsSize].data
                return DisplaySettings(
                    enableTimestamp,
                    fontSize
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.inflate(context, parent, settings)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lrcLyrics = lyric as? LrcLyrics
        if (lrcLyrics != null) {
            val highlighted = position == highlightedPosition
            holder.bind(
                line = lyricLines[position],
                timestamp = lyricTimestamps[position],
                dismiss = dismiss,
                highlighted = highlighted,
                words = if (highlighted) lrcLyrics.getLineWords(position) else emptyList(),
                highlightedWordIndex = if (highlighted) highlightedWordIndex else -1,
                highlightColor = highlightColor,
            )
        } else if (lyric is TextLyrics) {
            holder.bind(
                line = lyricLines[position],
                timestamp = -1,
                dismiss = dismiss,
                showTimestamp = false,
            )
        }
    }

    override fun getItemCount(): Int = lyric.length

}