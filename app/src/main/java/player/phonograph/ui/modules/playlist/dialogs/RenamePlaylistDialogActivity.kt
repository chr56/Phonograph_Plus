/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import player.phonograph.R
import player.phonograph.databinding.DialogRenamePlaylistBinding
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.playlist.Playlist
import player.phonograph.ui.basis.DialogActivity
import player.phonograph.util.parcelableExtra
import player.phonograph.util.ui.getScreenSize
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenamePlaylistDialogActivity : DialogActivity() {

    private lateinit var parameter: Parameter

    private var _binding: DialogRenamePlaylistBinding? = null
    private val binding: DialogRenamePlaylistBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        parameter = Parameter.fromLaunchingIntent(intent)
        super.onCreate(savedInstanceState)
        _binding = DialogRenamePlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.attributes = window.attributes.apply {
            width = getScreenSize().x / 7 * 6
        }


        with(binding) {
            name.editText!!.setText(parameter.playlist.name)

            buttonCancel.setOnClickListener { finish() }
            buttonRename.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val context = this@RenamePlaylistDialogActivity
                    rename(context, parameter.playlist, name.editText!!.text.toString())
                }
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    class Parameter(
        val playlist: Playlist,
    ) {
        companion object {
            private const val PLAYLIST = "playlist"

            fun fromLaunchingIntent(intent: Intent): Parameter {
                val playlist = intent.parcelableExtra<Playlist>(PLAYLIST)!!
                return Parameter(playlist)
            }

            fun buildLaunchingIntent(context: Context, playlist: Playlist): Intent =
                Intent(context, RenamePlaylistDialogActivity::class.java).apply {
                    putExtra(PLAYLIST, playlist)
                }
        }
    }

    companion object {
        private suspend fun rename(context: Context, playlist: Playlist, name: String) {
            if (name.trim().isNotEmpty()) {
                val result = PlaylistProcessors.writer(playlist)!!.rename(context, name)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (result) R.string.success else R.string.failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}