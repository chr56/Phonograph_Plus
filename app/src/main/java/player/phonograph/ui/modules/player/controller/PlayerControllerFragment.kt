/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.controller

import org.koin.androidx.viewmodel.ext.android.viewModel
import player.phonograph.model.service.PlayerState
import player.phonograph.model.ui.PlayerControllerStyle
import player.phonograph.model.ui.PlayerControllerStyle.Companion.ButtonPosition
import player.phonograph.model.ui.PlayerControllerStyle.Companion.FunctionType
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.util.component.MusicProgressUpdateDelegate
import player.phonograph.util.observe
import player.phonograph.util.parcelable
import util.theme.color.isColorLight
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import androidx.lifecycle.Lifecycle
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlin.getValue

abstract class PlayerControllerFragment<B : PlayerControllerBinding> : AbsMusicServiceFragment() {

    companion object {
        private const val ARGUMENT_BUTTONS = "arg_buttons"

        fun newInstance(configuration: PlayerControllerStyle): PlayerControllerFragment<*> {
            val controllerFragment = when (configuration.style) {
                PlayerControllerStyle.STYLE_FLAT    -> FlatStyled()
                PlayerControllerStyle.STYLE_CLASSIC -> ClassicStyled()
                else /* Default */                  -> FlatStyled()
            }
            controllerFragment.arguments = Bundle().apply {
                putParcelable(ARGUMENT_BUTTONS, configuration)
            }
            return controllerFragment
        }
    }

    protected abstract val binding: B

    protected var argumentConfiguration: PlayerControllerStyle? = null

    protected val panelViewModel: PanelViewModel by viewModel(ownerProducer = { requireActivity() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(musicProgressUpdateDelegate)
        argumentConfiguration = arguments?.parcelable<PlayerControllerStyle>(ARGUMENT_BUTTONS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return binding.createView(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.setupPlayPauseButton(view.context)
        binding.setUpProgressSlider()
        binding.setupProgressBarTextColor(view.context.primaryTextColor(true))

        for ((position, function) in readButtonConfiguration()) {
            binding.designate(function, position)
        }

        binding.commit(requireContext())

        observe(queueViewModel.repeatMode) { repeatMode -> binding.onUpdateRepeatModeIcon(repeatMode) }
        observe(queueViewModel.shuffleMode) { shuffleMode -> binding.onUpdateShuffleModeIcon(shuffleMode) }
        observe(MusicPlayerRemote.currentState) { state ->
            binding.onUpdatePlayerState(
                when (state) {
                    PlayerState.PAUSED    -> PlayerControllerBinding.STATE_PAUSED
                    PlayerState.PLAYING   -> PlayerControllerBinding.STATE_PLAYING
                    PlayerState.STOPPED   -> PlayerControllerBinding.STATE_STOPPED
                    PlayerState.PREPARING -> PlayerControllerBinding.STATE_BUFFERING
                }, shouldWithAnimation
            )
        }
        observe(panelViewModel.colorChange) { (oldColor, newColor) ->
            val context = requireContext()
            val oldControlsColor = context.secondaryTextColor(!isColorLight(oldColor))
            val newControlsColor = context.secondaryTextColor(!isColorLight(newColor))
            binding.onUpdateColor(oldControlsColor, newControlsColor, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.destroyView()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        binding.onUpdatePlayerState(PlayerControllerBinding.STATE_BUFFERING, shouldWithAnimation)
    }

    override fun onServiceDisconnected() {
        super.onServiceDisconnected()
        binding.onUpdatePlayerState(PlayerControllerBinding.STATE_STOPPED, shouldWithAnimation)
    }

    protected val shouldWithAnimation get() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    abstract fun provideRippleCenter(): Point?

    fun onShow() {
        binding.onShow(isResumed)
    }

    fun onHide() {
        binding.onHide(isResumed)
    }

    protected fun readButtonConfiguration(): Map<@ButtonPosition Int, @FunctionType Int> =
        argumentConfiguration?.buttons ?: PlayerControllerStyle.DEFAULT_BUTTONS

    //region Progress
    private val musicProgressUpdateDelegate = MusicProgressUpdateDelegate(::onUpdateProgress)
    private fun onUpdateProgress(progress: Int, total: Int) = binding.onUpdateProgressViews(progress, total)
    //endregion


    class FlatStyled : PlayerControllerFragment<PlayerControllerFlatStyledBinding>() {
        override val binding: PlayerControllerFlatStyledBinding = PlayerControllerFlatStyledBinding()

        override fun provideRippleCenter(): Point? = null
    }

    class ClassicStyled : PlayerControllerFragment<PlayerControllerClassicStyledBinding>() {
        override val binding: PlayerControllerClassicStyledBinding = PlayerControllerClassicStyledBinding()

        var fabElevation: Float
            get() = binding.centralButton.elevation
            set(value) {
                binding.centralButton.elevation = value
            }

        override fun provideRippleCenter(): Point {
            val fab = binding.centralButton
            val x = fab.x + fab.width / 2
            val y = fab.y + fab.height / 2
            return Point(x.toInt(), y.toInt())
        }
    }
}
