package player.phonograph.ui.modules.panel

import player.phonograph.foundation.warning
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.content.Context
import android.os.Bundle
import android.view.View

/**
 * Fragments that supports receiving service binding events.
 *
 * This Fragment must be attached to a proper [AbsMusicServiceActivity] to work!
 *
 */
abstract class AbsMusicServiceFragment : Fragment(), MusicServiceEventListener {

    private var attachedContext: Context? = null
    val attachedMusicServiceActivity get() = attachedContext as? AbsMusicServiceActivity

    protected val queueViewModel: QueueViewModel by viewModels({ requireActivity() })

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AbsMusicServiceActivity) {
            attachedContext = context
        } else {
            warning(
                javaClass.simpleName,
                "Parent ${context.javaClass.simpleName} is not `${AbsMusicServiceActivity::class.java.simpleName}`!"
            )
        }
    }

    override fun onDetach() {
        attachedContext = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (attachedContext as? AbsMusicServiceActivity)?.addMusicServiceEventListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (attachedContext as? AbsMusicServiceActivity)?.removeMusicServiceEventListener(this)
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
}
