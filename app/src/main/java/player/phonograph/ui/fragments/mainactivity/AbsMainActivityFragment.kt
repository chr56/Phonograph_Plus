package player.phonograph.ui.fragments.mainactivity

import android.os.Bundle
import androidx.fragment.app.Fragment
import player.phonograph.ui.activities.MainActivity

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMainActivityFragment : Fragment() {
    val mainActivity: MainActivity
        get() = requireActivity() as MainActivity

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    } // Todo life-cycle
}
