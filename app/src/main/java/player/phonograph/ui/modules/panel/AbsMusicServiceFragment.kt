package player.phonograph.ui.modules.panel

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.content.Context
import android.os.Bundle
import android.view.View
import kotlin.getValue

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceFragment : Fragment(), MusicServiceEventListener {

    private var _bindingActivity: AbsMusicServiceActivity? = null
    val bindingActivity get() = _bindingActivity!!

    protected val queueViewModel: QueueViewModel by viewModels({ requireActivity() })

    override fun onAttach(context: Context) {
        super.onAttach(context)
        require(context is AbsMusicServiceActivity) {
            "${context.javaClass.simpleName} must be `${AbsMusicServiceActivity::class.java.simpleName}`," +
                    " so `AbsMusicServiceFragment` can be bind!"
        }
        _bindingActivity = context
    }

    override fun onDetach() {
        _bindingActivity = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingActivity.addMusicServiceEventListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingActivity.removeMusicServiceEventListener(this)
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
}
