package player.phonograph.ui.fragments.player.card;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import chr_56.MDthemer.util.ColorUtil;
import chr_56.MDthemer.util.MaterialColorHelper;
import chr_56.MDthemer.util.TintHelper;
import player.phonograph.R;
import player.phonograph.databinding.FragmentCardPlayerPlaybackControlsBinding;
import player.phonograph.helper.MusicPlayerRemote;
import player.phonograph.helper.MusicProgressViewUpdateHelper;
import player.phonograph.helper.PlayPauseButtonOnClickHandler;
import player.phonograph.misc.SimpleOnSeekbarChangeListener;
import player.phonograph.service.MusicService;
import player.phonograph.ui.fragments.AbsMusicServiceFragment;
import player.phonograph.util.MusicUtil;
import player.phonograph.views.PlayPauseDrawable;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class CardPlayerPlaybackControlsFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {

    protected FragmentCardPlayerPlaybackControlsBinding viewBinding;

    private PlayPauseDrawable playerFabPlayPauseDrawable;

    private int lastPlaybackControlsColor;
    private int lastDisabledPlaybackControlsColor;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentCardPlayerPlaybackControlsBinding.inflate(inflater);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpMusicControllers();
        updateProgressTextColor();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        progressViewUpdateHelper.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressViewUpdateHelper.stop();
    }

    @Override
    public void onServiceConnected() {
        updatePlayPauseDrawableState(false);
        updateRepeatState();
        updateShuffleState();
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
    }

    @Override
    public void onRepeatModeChanged() {
        updateRepeatState();
    }

    @Override
    public void onShuffleModeChanged() {
        updateShuffleState();
    }

    public void setDark(boolean dark) {
        if (dark) {
            lastPlaybackControlsColor = MaterialColorHelper.getSecondaryTextColor(getActivity(), true);
            lastDisabledPlaybackControlsColor = MaterialColorHelper.getSecondaryDisabledTextColor(getActivity(), true);
        } else {
            lastPlaybackControlsColor = MaterialColorHelper.getPrimaryTextColor(getActivity(), false);
            lastDisabledPlaybackControlsColor = MaterialColorHelper.getPrimaryDisabledTextColor(getActivity(), false);
        }

        updateRepeatState();
        updateShuffleState();
        updatePrevNextColor();
        updateProgressTextColor();
    }

    private void setUpPlayPauseFab() {
        final int fabColor = Color.WHITE;
        TintHelper.setTintAuto(viewBinding.playerPlayPauseFab, fabColor, true);

        playerFabPlayPauseDrawable = new PlayPauseDrawable(requireActivity());

        viewBinding.playerPlayPauseFab.setImageDrawable(playerFabPlayPauseDrawable); // Note: set the drawable AFTER TintHelper.setTintAuto() was called
        viewBinding.playerPlayPauseFab.setColorFilter(MaterialColorHelper.getPrimaryTextColor(getContext(), ColorUtil.isColorLight(fabColor)), PorterDuff.Mode.SRC_IN);
        viewBinding.playerPlayPauseFab.setOnClickListener(new PlayPauseButtonOnClickHandler());
        viewBinding.playerPlayPauseFab.post(() -> {
            viewBinding.playerPlayPauseFab.setPivotX((float) viewBinding.playerPlayPauseFab.getWidth() / 2);
            viewBinding.playerPlayPauseFab.setPivotY((float) viewBinding.playerPlayPauseFab.getHeight() / 2);
        });
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            playerFabPlayPauseDrawable.setPause(animate);
        } else {
            playerFabPlayPauseDrawable.setPlay(animate);
        }
    }

    private void setUpMusicControllers() {
        setUpPlayPauseFab();
        setUpPrevNext();
        setUpRepeatButton();
        setUpShuffleButton();
        setUpProgressSlider();
    }

    private void setUpPrevNext() {
        updatePrevNextColor();
        viewBinding.playerNextButton.setOnClickListener(v -> MusicPlayerRemote.playNextSong());
        viewBinding.playerPrevButton.setOnClickListener(v -> MusicPlayerRemote.back());
    }

    private void updateProgressTextColor() {
        int color = MaterialColorHelper.getPrimaryTextColor(getContext(), false);
        viewBinding.playerSongTotalTime.setTextColor(color);
        viewBinding.playerSongCurrentProgress.setTextColor(color);
    }

    private void updatePrevNextColor() {
        viewBinding.playerNextButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        viewBinding.playerPrevButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
    }

    private void setUpShuffleButton() {
        viewBinding.playerShuffleButton.setOnClickListener(v -> MusicPlayerRemote.toggleShuffleMode());
    }

    private void updateShuffleState() {
        switch (MusicPlayerRemote.getShuffleMode()) {
            case MusicService.SHUFFLE_MODE_SHUFFLE:
                viewBinding.playerShuffleButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            default:
                viewBinding.playerShuffleButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
        }
    }

    private void setUpRepeatButton() {
        viewBinding.playerRepeatButton.setOnClickListener(v -> MusicPlayerRemote.cycleRepeatMode());
    }

    private void updateRepeatState() {
        switch (MusicPlayerRemote.getRepeatMode()) {
            case MusicService.REPEAT_MODE_NONE:
                viewBinding.playerRepeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                viewBinding.playerRepeatButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            case MusicService.REPEAT_MODE_ALL:
                viewBinding.playerRepeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                viewBinding.playerRepeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            case MusicService.REPEAT_MODE_THIS:
                viewBinding.playerRepeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                viewBinding.playerRepeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
        }
    }

    public void show() {
        viewBinding.playerPlayPauseFab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .rotation(360f)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public void hide() {
        viewBinding.playerPlayPauseFab.setScaleX(0f);
        viewBinding.playerPlayPauseFab.setScaleY(0f);
        viewBinding.playerPlayPauseFab.setRotation(0f);
    }

    private void setUpProgressSlider() {
        int color = MaterialColorHelper.getPrimaryTextColor(getContext(), false);
        viewBinding.playerProgressSlider.getThumb().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        viewBinding.playerProgressSlider.getProgressDrawable().mutate().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);

        viewBinding.playerProgressSlider.setOnSeekBarChangeListener(new SimpleOnSeekbarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MusicPlayerRemote.seekTo(progress);
                    onUpdateProgressViews(MusicPlayerRemote.getSongProgressMillis(), MusicPlayerRemote.getSongDurationMillis());
                }
            }
        });
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {
        viewBinding.playerProgressSlider.setMax(total);
        viewBinding.playerProgressSlider.setProgress(progress);
        viewBinding.playerSongTotalTime.setText(MusicUtil.getReadableDurationString(total));
        viewBinding.playerSongCurrentProgress.setText(MusicUtil.getReadableDurationString(progress));
    }
}
