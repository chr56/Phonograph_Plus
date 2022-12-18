package player.phonograph.ui.dialogs

import lib.phonograph.dialog.alertDialog
import mt.pref.ThemeColor.accentColor
import player.phonograph.R
import player.phonograph.mediastore.DeleteSongUtil
import player.phonograph.misc.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.ui.components.ViewComponent
import player.phonograph.ui.components.viewcreater.*
import player.phonograph.util.CoroutineUtil
import player.phonograph.util.StringUtil
import player.phonograph.util.Util
import player.phonograph.util.permissions.hasStorageWritePermission
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.core.view.setMargins
import androidx.fragment.app.DialogFragment
import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad), chr_56<modify>
 */
class DeleteSongsDialog : DialogFragment() {


    private lateinit var songs: ArrayList<Song>
    private var hasPermission: Boolean = false

    private lateinit var model: DeleteSongsModel

    private lateinit var window: DeleteSongsWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songs = requireArguments().getParcelableArrayList("songs")!!
        hasPermission = hasStorageWritePermission(requireContext())
        model = DeleteSongsModel(songs, hasPermission)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = container ?: FrameLayout(requireContext())
        window = DeleteSongsWindow(requireActivity(), this::dismiss)
        window.inflate(root, inflater)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        window.loadData(model)
    }

    class DeleteSongsWindow(
        private val activity: Activity,
        val dismiss: () -> Unit,
    ) : ViewComponent<ViewGroup, DeleteSongsModel> {

        lateinit var titlePanel: TitlePanel
        lateinit var buttonPanel: ButtonPanel
        lateinit var contentPanel: ContentPanel

        lateinit var contentText: TextView

        @SuppressLint("RestrictedApi")
        override fun inflate(rootContainer: ViewGroup, layoutInflater: LayoutInflater?) {
            val accentColor = accentColor(activity)

            titlePanel = titlePanel(activity)

            buttonPanel = buttonPanel(activity) {
                // orientation = LinearLayout.VERTICAL
                button(0, activity.getString(R.string.grant_permission), accentColor) {
                    navigateToStorageSetting(activity)
                }
                button(1, activity.getString(R.string.delete_action), accentColor) {
                    dismiss()
                    coroutineScope.launch {
                        delete()
                    }
                }
                space(2)
                button(3, activity.getString(R.string.delete_with_lyrics), accentColor) {
                    dismiss()
                    coroutineScope.launch {
                        deleteWithLyrics()
                    }
                }
            }


            contentPanel = contentPanel(activity) {
                this@DeleteSongsWindow.contentText = TextView(activity).apply {
                    textSize = 16f
                }
                addView(contentText)
            }

            val root = buildDialogView(activity, titlePanel, contentPanel, buttonPanel)
            rootContainer.addView(root,
                                  FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                                      .apply { setMargins(24) })
        }

        override fun loadData(model: DeleteSongsModel) {
            contentText.text =
                StringUtil.buildDeletionMessage(
                    context = activity,
                    itemSize = model.songs.size,
                    extraSuffix = if (!model.hasPermission) activity.getString(
                        R.string.permission_manage_external_storage_denied
                    ) else "",
                    StringUtil.ItemGroup(
                        activity.resources.getQuantityString(R.plurals.item_songs,
                                                             model.songs.size),
                        model.songs.map { it.title }
                    )
                )
            titlePanel.titleView.text = activity.getString(R.string.delete_action)
            delete = {
                val total = model.songs.size
                val fails = DeleteSongUtil.deleteSongs(activity, model.songs)

                val msg: String =
                    activity.resources.getQuantityString(
                        R.plurals.msg_deletion_result,
                        total,
                        total - fails.size,
                        total
                    )

                if (fails.isNotEmpty()) Handler(Looper.getMainLooper()).post {
                    // handle fail , report and try again
                    showFailDialog(activity, msg, fails)
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                }
            }
            deleteWithLyrics = {
                deleteLyrics(activity, model.songs)
                delete()
            }
        }

        override fun destroy() {}

        var delete: () -> Unit = {}
        var deleteWithLyrics: () -> Unit = { }
        private fun deleteLyrics(activity: Activity, songs: ArrayList<Song>) {
            for (song in songs) {
                val file = File(song.data)
                val preciseFiles = LyricsLoader.getExternalPreciseLyricsFile(file)
                val fails = mutableListOf<String>()
                preciseFiles.forEach {
                    val result = it.delete()
                    if (!result) fails.add(it.name)
                }
                if (fails.isNotEmpty()) {
                    Util.withLooper {
                        Toast.makeText(activity,
                                       activity.getString(R.string.failed_to_delete) + fails.fold("") { a, n -> "$a,$n" },
                                       Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        private fun showFailDialog(context: Activity, msg: String, failList: List<Song>) {
            alertDialog(context) {
                val t = context.getString(R.string.failed_to_delete)
                title(t)
                message(
                    buildString {
                        append("$msg\n")
                        append("$t: \n")
                        append(failList.fold("") { acc, song -> "$acc${song.title}\n" })
                    }
                )
                neutralButton(R.string.grant_permission) {
                    navigateToStorageSetting(context)
                }
                positiveButton(android.R.string.ok) { dialog ->
                    dialog.dismiss()
                }
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    negativeButton(R.string.retry) {
                        val uris = failList.map { song ->
                            Uri.withAppendedPath(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id.toString()
                            )
                        }
                        context.startIntentSenderForResult(
                            MediaStore.createDeleteRequest(
                                context.contentResolver, uris
                            ).intentSender, 0, null, 0, 0, 0
                        )
                    }
                }
            }.show()
        }
    }

    class DeleteSongsModel(val songs: ArrayList<Song>, var hasPermission: Boolean)


    override fun onStart() {
        requireDialog().window!!.attributes =
            requireDialog().window!!.let { window ->
                window.attributes.apply {
                    width = (requireActivity().window.decorView.width * 0.90).toInt()
                }
            }
        super.onStart()
    }

    companion object {
        private const val TAG = "DeleteSongsDialog"
        private val coroutineScope =
            CoroutineScope(
                Dispatchers.IO +
                        CoroutineUtil.createDefaultExceptionHandler(TAG, "Fail!")
            )

        fun create(songs: ArrayList<Song>): DeleteSongsDialog =
            DeleteSongsDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("songs", songs)
                }
            }
    }
}
