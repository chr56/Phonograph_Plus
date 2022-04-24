package player.phonograph.ui.fragments.mainactivity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import player.phonograph.ui.activities.MainActivity

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMainActivityFragment : Fragment() {
    val mainActivity: MainActivity
        get() = requireActivity() as MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    super.onCreate(owner)
                    setHasOptionsMenu(true)
                }
            }
        )
    }
}
