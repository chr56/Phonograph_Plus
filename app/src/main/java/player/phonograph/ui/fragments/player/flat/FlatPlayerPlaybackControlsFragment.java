package player.phonograph.ui.fragments.player.flat;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.util.Collection;
import java.util.LinkedList;

import chr_56.MDthemer.util.MaterialColorHelper;
import player.phonograph.R;
import player.phonograph.databinding.FragmentFlatPlayerPlaybackControlsBinding;
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
public class FlatPlayerPlaybackControlsFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {

    private FragmentFlatPlayerPlaybackControlsBinding viewBinding;

    private PlayPauseDrawable playPauseDrawable;

    private int lastPlaybackControlsColor;
    private int lastDisabledPlaybackControlsColor;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    private AnimatorSet musicControllerAnimationSet;

    private boolean hidden = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentFlatPlayerPlaybackControlsBinding.inflate(inflater);
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
        updatePlayPauseColor();
        updateProgressTextColor();
    }

    private void setUpPlayPauseButton() {
        playPauseDrawable = new PlayPauseDrawable(requireActivity());
        viewBinding.playerPlayPauseButton.setImageDrawable(playPauseDrawable);
        updatePlayPauseColor();
        viewBinding.playerPlayPauseButton.setOnClickListener(new PlayPauseButtonOnClickHandler());
        viewBinding.playerPlayPauseButton.post(() -> {
            viewBinding.playerPlayPauseButton.setPivotX((float) viewBinding.playerPlayPauseButton.getWidth() / 2);
            viewBinding.playerPlayPauseButton.setPivotY((float) viewBinding.playerPlayPauseButton.getHeight() / 2);
        });
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            playPauseDrawable.setPause(animate);
        } else {
            playPauseDrawable.setPlay(animate);
        }
    }

    private void setUpMusicControllers() {
        setUpPlayPauseButton();
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

    private void updatePlayPauseColor() {
        viewBinding.playerPlayPauseButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
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
        if (hidden) {
            if (musicControllerAnimationSet == null) {
                TimeInterpolator interpolator = new FastOutSlowInInterpolator();
                final int duration = 300;

                LinkedList<Animator> animators = new LinkedList<>();

                addAnimation(animators, viewBinding.playerPlayPauseButton, interpolator, duration, 0);
                addAnimation(animators, viewBinding.playerNextButton, interpolator, duration, 100);
                addAnimation(animators, viewBinding.playerPrevButton, interpolator, duration, 100);
                addAnimation(animators, viewBinding.playerRepeatButton, interpolator, duration, 200);
                addAnimation(animators, viewBinding.playerShuffleButton, interpolator, duration, 200);

                musicControllerAnimationSet = new AnimatorSet();
                musicControllerAnimationSet.playTogether(animators);
            } else {
                musicControllerAnimationSet.cancel();
            }
            musicControllerAnimationSet.start();
        }

        hidden = false;
    }

    public void hide() {
        if (musicControllerAnimationSet != null) {
            musicControllerAnimationSet.cancel();
        }
        prepareForAnimation(viewBinding.playerPlayPauseButton);
        prepareForAnimation(viewBinding.playerNextButton);
        prepareForAnimation(viewBinding.playerPrevButton);
        prepareForAnimation(viewBinding.playerRepeatButton);
        prepareForAnimation(viewBinding.playerShuffleButton);

        hidden = true;
    }

    private static void addAnimation(Collection<Animator> animators, View view, TimeInterpolator interpolator, int duration, int delay) {
        Animator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f);
        scaleX.setInterpolator(interpolator);
        scaleX.setDuration(duration);
        scaleX.setStartDelay(delay);
        animators.add(scaleX);

        Animator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f);
        scaleY.setInterpolator(interpolator);
        scaleY.setDuration(duration);
        scaleY.setStartDelay(delay);
        animators.add(scaleY);
    }

    private static void prepareForAnimation(View view) {
        if (view != null) {
            view.setScaleX(0f);
            view.setScaleY(0f);
        }
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
