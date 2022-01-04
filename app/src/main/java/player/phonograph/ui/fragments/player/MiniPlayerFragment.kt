package player.phonograph.ui.fragments.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import chr_56.MDthemer.core.ThemeColor;
import chr_56.MDthemer.util.Util;
import player.phonograph.R;
import player.phonograph.databinding.FragmentMiniPlayerBinding;
import player.phonograph.helper.MusicPlayerRemote;
import player.phonograph.helper.MusicProgressViewUpdateHelper;
import player.phonograph.helper.PlayPauseButtonOnClickHandler;
import player.phonograph.ui.fragments.AbsMusicServiceFragment;
import player.phonograph.views.PlayPauseDrawable;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MiniPlayerFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {

    private FragmentMiniPlayerBinding viewBinding;

    private PlayPauseDrawable miniPlayerPlayPauseDrawable;
    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        viewBinding = FragmentMiniPlayerBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.setOnTouchListener(new FlingPlayBackController(getActivity()));
        setUpMiniPlayer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }


    private void setUpMiniPlayer() {
        setUpPlayPauseButton();
        viewBinding.progressIndicator.setIndicatorColor(ThemeColor.accentColor(requireContext()));
    }

    private void setUpPlayPauseButton() {
        miniPlayerPlayPauseDrawable = new PlayPauseDrawable(requireContext());
        viewBinding.miniPlayerPlayPauseButton.setImageDrawable(miniPlayerPlayPauseDrawable);
        viewBinding.miniPlayerPlayPauseButton.setColorFilter(Util.resolveColor(requireActivity(), R.attr.iconColor, ThemeColor.textColorSecondary(requireActivity())), PorterDuff.Mode.SRC_IN);
        viewBinding.miniPlayerPlayPauseButton.setOnClickListener(new PlayPauseButtonOnClickHandler());
    }

    private void updateSongTitle() {
        viewBinding.miniPlayerTitle.setText(MusicPlayerRemote.getCurrentSong().title);
    }

    @Override
    public void onServiceConnected() {
        updateSongTitle();
        updatePlayPauseDrawableState(false);
    }

    @Override
    public void onPlayingMetaChanged() {
        updateSongTitle();
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {
        viewBinding.progressIndicator.setMax(total);
        viewBinding.progressIndicator.setProgress(progress);
        viewBinding.progressIndicator.show();
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

    private static class FlingPlayBackController implements View.OnTouchListener {

        GestureDetector flingPlayBackController;

        public FlingPlayBackController(Context context) {
            flingPlayBackController = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (Math.abs(velocityX) > Math.abs(velocityY)) {
                        if (velocityX < 0) {
                            MusicPlayerRemote.playNextSong();
                            return true;
                        } else if (velocityX > 0) {
                            MusicPlayerRemote.playPreviousSong();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return flingPlayBackController.onTouchEvent(event);
        }
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            miniPlayerPlayPauseDrawable.setPause(animate);
        } else {
            miniPlayerPlayPauseDrawable.setPlay(animate);
        }
    }
}
