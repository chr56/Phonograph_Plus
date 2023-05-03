package player.phonograph.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import player.phonograph.model.MusicServiceEventListener
import player.phonograph.ui.activities.base.AbsMusicServiceActivity

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class AbsMusicServiceFragment : Fragment(), MusicServiceEventListener {
    private var bindingActivity: AbsMusicServiceActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            bindingActivity = context as AbsMusicServiceActivity
        } catch (e: ClassCastException) {
            throw RuntimeException(context.javaClass.simpleName + " must be an instance of " + AbsMusicServiceActivity::class.java.simpleName)
        }
    }
    override fun onDetach() {
        super.onDetach()
        bindingActivity = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingActivity!!.addMusicServiceEventListener(this)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        bindingActivity!!.removeMusicServiceEventListener(this)
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
}
