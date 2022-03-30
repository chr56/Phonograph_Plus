package player.phonograph.ui.fragments.player.card;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import player.phonograph.util.FavoriteUtil;
import util.mdcolor.pref.ThemeColor;
import util.mdcolor.ColorUtil;
import util.mddesign.util.ToolbarColorUtil;
import player.phonograph.R;
import player.phonograph.adapter.base.MediaEntryViewHolder;
import player.phonograph.adapter.song.PlayingQueueAdapter;
import player.phonograph.databinding.FragmentCardPlayerBinding;
import player.phonograph.dialogs.LyricsDialog;
import player.phonograph.dialogs.SongShareDialog;
import player.phonograph.helper.MusicPlayerRemote;
import player.phonograph.helper.menu.SongMenuHelper;
import player.phonograph.model.Song;
import player.phonograph.model.lyrics.AbsLyrics;
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity;
import player.phonograph.ui.fragments.player.AbsPlayerFragment;
import player.phonograph.ui.fragments.player.PlayerAlbumCoverFragment;
import player.phonograph.util.ImageUtil;
import player.phonograph.util.LyricsUtil;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.Util;
import player.phonograph.util.ViewUtil;
import player.phonograph.views.WidthFitSquareLayout;

public class CardPlayerFragment extends AbsPlayerFragment implements PlayerAlbumCoverFragment.Callbacks, SlidingUpPanelLayout.PanelSlideListener {


    private int lastColor;

    protected FragmentCardPlayerBinding viewBinding;

    private CardPlayerPlaybackControlsFragment playbackControlsFragment;
    private PlayerAlbumCoverFragment playerAlbumCoverFragment;

    private LinearLayoutManager layoutManager;

    private PlayingQueueAdapter playingQueueAdapter;

    private RecyclerView.Adapter wrappedAdapter;
    private RecyclerViewDragDropManager recyclerViewDragDropManager;

    private AsyncTask updateIsFavoriteTask;
    private AsyncTask updateLyricsAsyncTask;

    private AbsLyrics lyrics;

    private Impl impl;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Util.isLandscape(getResources())) {
            impl = new LandscapeImpl(this);
        } else {
            impl = new PortraitImpl(this);
        }

        viewBinding = FragmentCardPlayerBinding.inflate(inflater);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        impl.init();

        setUpPlayerToolbar();
        setUpSubFragments();

        setUpRecyclerView();

        viewBinding.playerSlidingLayout.addPanelSlideListener(this);
        viewBinding.playerSlidingLayout.setAntiDragView(view.findViewById(R.id.draggable_area));

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                impl.setUpPanelAndAlbumCoverHeight();
            }
        });

        // for some reason the xml attribute doesn't get applied here.
        viewBinding.playingQueueCard.setCardBackgroundColor(util.mddesign.util.Util.resolveColor(getActivity(), R.attr.cardBackgroundColor));
    }

    @Override
    public void onDestroyView() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager.release();
            recyclerViewDragDropManager = null;
        }

        viewBinding.playerRecyclerView.setItemAnimator(null);
        viewBinding.playerRecyclerView.setAdapter(null);


        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter);
            wrappedAdapter = null;
        }
        playingQueueAdapter = null;
        layoutManager = null;
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onPause() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager.cancelDrag();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkToggleToolbar(viewBinding.toolbarContainer);
    }

    @Override
    public void onServiceConnected() {
        updateQueue();
        updateCurrentSong();
        updateIsFavorite();
        updateLyrics();
    }

    @Override
    public void onPlayingMetaChanged() {
        updateCurrentSong();
        updateIsFavorite();
        updateQueuePosition();
        updateLyrics();
    }

    @Override
    public void onQueueChanged() {
        updateQueue();
    }

    @Override
    public void onMediaStoreChanged() {
        updateQueue();
        updateIsFavorite();
    }

    private void updateQueue() {
        playingQueueAdapter.swapDataSet(MusicPlayerRemote.getPlayingQueue(), MusicPlayerRemote.getPosition());
        viewBinding.playerQueueSubHeader.setText(getUpNextAndQueueTime());
        if (viewBinding.playerSlidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            resetToCurrentPosition();
        }
    }

    private void updateQueuePosition() {
        playingQueueAdapter.setCurrent(MusicPlayerRemote.getPosition());
        viewBinding.playerQueueSubHeader.setText(getUpNextAndQueueTime());
        if (viewBinding.playerSlidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            resetToCurrentPosition();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void updateCurrentSong() {
        impl.updateCurrentSong(MusicPlayerRemote.getCurrentSong());
    }

    private void setUpSubFragments() {
        playbackControlsFragment = (CardPlayerPlaybackControlsFragment) getChildFragmentManager().findFragmentById(R.id.playback_controls_fragment);
        playerAlbumCoverFragment = (PlayerAlbumCoverFragment) getChildFragmentManager().findFragmentById(R.id.player_album_cover_fragment);

        playerAlbumCoverFragment.setCallbacks(this);
    }

    private void setUpPlayerToolbar() {
        viewBinding.playerToolbar.inflateMenu(R.menu.menu_player);
        viewBinding.playerToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        viewBinding.playerToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        viewBinding.playerToolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_lyrics:
                if (lyrics != null)
                    LyricsDialog.create(lyrics, MusicPlayerRemote.getCurrentSong()).show(requireActivity().getSupportFragmentManager(), "LYRICS");
                return true;
        }
        return super.onMenuItemClick(item);
    }

    private void setUpRecyclerView() {
        recyclerViewDragDropManager = new RecyclerViewDragDropManager();
        final GeneralItemAnimator animator = new RefactoredDefaultItemAnimator();

        playingQueueAdapter = new PlayingQueueAdapter(
                ((AppCompatActivity) requireActivity()),
                MusicPlayerRemote.getPlayingQueue(),
                MusicPlayerRemote.getPosition(),
                R.layout.item_list,
                false,
                null);
        wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(playingQueueAdapter);

        layoutManager = new LinearLayoutManager(getActivity());

        viewBinding.playerRecyclerView.setLayoutManager(layoutManager);
        viewBinding.playerRecyclerView.setAdapter(wrappedAdapter);
        viewBinding.playerRecyclerView.setItemAnimator(animator);

        recyclerViewDragDropManager.attachRecyclerView(viewBinding.playerRecyclerView);

        layoutManager.scrollToPositionWithOffset(MusicPlayerRemote.getPosition() + 1, 0);
    }


    @SuppressLint("StaticFieldLeak") //TODO StaticFieldLeak
    private void updateIsFavorite() {
        if (updateIsFavoriteTask != null) updateIsFavoriteTask.cancel(false);
        updateIsFavoriteTask = new AsyncTask<Song, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Song... params) {
                Activity activity = getActivity();
                if (activity != null) {
                    return FavoriteUtil.isFavorite(getActivity(), params[0]);
                } else {
                    cancel(false);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Boolean isFavorite) {
                Activity activity = getActivity();
                if (activity != null) {
                    int res = isFavorite ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_border_white_24dp;
                    int color = ToolbarColorUtil.toolbarContentColor(activity, Color.TRANSPARENT);
                    Drawable drawable = ImageUtil.getTintedVectorDrawable(activity, res, color);
                    viewBinding.playerToolbar.getMenu().findItem(R.id.action_toggle_favorite)
                            .setIcon(drawable)
                            .setTitle(isFavorite ? getString(R.string.action_remove_from_favorites) : getString(R.string.action_add_to_favorites));
                }
            }
        }.execute(MusicPlayerRemote.getCurrentSong());
    }

    @SuppressLint("StaticFieldLeak") //TODO StaticFieldLeak
    private void updateLyrics() {
        if (updateLyricsAsyncTask != null) updateLyricsAsyncTask.cancel(false);
        final Song song = MusicPlayerRemote.getCurrentSong();
        updateLyricsAsyncTask = new AsyncTask<Void, Void, AbsLyrics>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                lyrics = null;
                playerAlbumCoverFragment.setLyrics(null);
                viewBinding.playerToolbar.getMenu().removeItem(R.id.action_show_lyrics);
            }

            @Override
            protected AbsLyrics doInBackground(Void... params) {
                return LyricsUtil.fetchLyrics(song);
            }

            @Override
            protected void onPostExecute(AbsLyrics l) {
                lyrics = l;
                playerAlbumCoverFragment.setLyrics(lyrics);
                if (lyrics == null) {
                    viewBinding.playerToolbar.getMenu().removeItem(R.id.action_show_lyrics);
                } else {
                    Activity activity = getActivity();
                    if (activity != null)
                        if (viewBinding.playerToolbar.getMenu().findItem(R.id.action_show_lyrics) == null) {
                            int color = ToolbarColorUtil.toolbarContentColor(activity, Color.TRANSPARENT);
                            Drawable drawable = ImageUtil.getTintedVectorDrawable(activity, R.drawable.ic_comment_text_outline_white_24dp, color);
                            viewBinding.playerToolbar.getMenu()
                                    .add(Menu.NONE, R.id.action_show_lyrics, Menu.NONE, R.string.action_show_lyrics)
                                    .setIcon(drawable)
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        }
                }
            }

            @Override
            protected void onCancelled(AbsLyrics s) {
                onPostExecute(null);
            }
        }.execute();
    }

    @Override
    @ColorInt
    public int getPaletteColor() {
        return lastColor;
    }

    private void animateColorChange(final int newColor) {
        impl.animateColorChange(newColor);
        lastColor = newColor;
    }

    @Override
    protected void toggleFavorite(Song song) {
        super.toggleFavorite(song);
        if (song.id == MusicPlayerRemote.getCurrentSong().id) {
            if (FavoriteUtil.isFavorite(requireActivity(), song)) {
                playerAlbumCoverFragment.showHeartAnimation();
            }
            updateIsFavorite();
        }
    }

    @Override
    public void onShow() {
        playbackControlsFragment.show();
    }

    @Override
    public void onHide() {
        playbackControlsFragment.hide();
        onBackPressed();
    }

    @Override
    public boolean onBackPressed() {
        boolean wasExpanded = false;
        wasExpanded = viewBinding.playerSlidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
        viewBinding.playerSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        return wasExpanded;
    }

    @Override
    public void onColorChanged(int color) {
        animateColorChange(color);
        playbackControlsFragment.setDark(ColorUtil.isColorLight(color));
        getCallbacks().onPaletteColorChanged();
    }

    @Override
    public void onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.getCurrentSong());
    }

    @Override
    public void onToolbarToggled() {
        toggleToolbar(viewBinding.toolbarContainer);
    }

    @Override
    public void onPanelSlide(View view, float slide) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float density = getResources().getDisplayMetrics().density;

            float cardElevation = (6 * slide + 2) * density;
            if (!isValidElevation(cardElevation))
                return; // we have received some crash reports in setCardElevation()
            viewBinding.playingQueueCard.setCardElevation(cardElevation);

            float buttonElevation = (2 * Math.max(0, (1 - (slide * 16))) + 2) * density;
            if (!isValidElevation(buttonElevation)) return;
            playbackControlsFragment.viewBinding.playerPlayPauseFab.setElevation(buttonElevation);
        }
    }

    private boolean isValidElevation(float elevation) {
        return elevation >= -Float.MAX_VALUE && elevation <= Float.MAX_VALUE;
    }

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
        switch (newState) {
            case COLLAPSED:
                onPanelCollapsed(panel);
                break;
            case ANCHORED:
                viewBinding.playerSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED); // this fixes a bug where the panel would get stuck for some reason
                break;
        }
    }

    public void onPanelCollapsed(View panel) {
        resetToCurrentPosition();
    }

    private void resetToCurrentPosition() {
        viewBinding.playerRecyclerView.stopScroll();
        layoutManager.scrollToPositionWithOffset(MusicPlayerRemote.getPosition() + 1, 0);
    }

    interface Impl {
        void init();

        void updateCurrentSong(Song song);

        void animateColorChange(final int newColor);

        void setUpPanelAndAlbumCoverHeight();
    }

    private static abstract class BaseImpl implements Impl {
        protected CardPlayerFragment fragment;

        public BaseImpl(CardPlayerFragment fragment) {
            this.fragment = fragment;
        }

        public AnimatorSet createDefaultColorChangeAnimatorSet(int newColor) {
            Animator backgroundAnimator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //noinspection ConstantConditions
                int x = (int) (fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.getX() + fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.getWidth() / 2 + fragment.playbackControlsFragment.getView().getX());
                int y = (int) (fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.getY() + fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.getHeight() / 2 + fragment.playbackControlsFragment.getView().getY() + fragment.playbackControlsFragment.viewBinding.playerProgressSlider.getHeight());
                float startRadius = Math.max(fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.getWidth() / 2, fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.getHeight() / 2);
                float endRadius = Math.max(fragment.viewBinding.colorBackground.getWidth(), fragment.viewBinding.colorBackground.getHeight());
                fragment.viewBinding.colorBackground.setBackgroundColor(newColor);
                backgroundAnimator = ViewAnimationUtils.createCircularReveal(fragment.viewBinding.colorBackground, x, y, startRadius, endRadius);
            } else {
                backgroundAnimator = ViewUtil.createBackgroundColorTransition(fragment.viewBinding.colorBackground, fragment.lastColor, newColor);
            }

            AnimatorSet animatorSet = new AnimatorSet();

            animatorSet.play(backgroundAnimator);

            if (!util.mddesign.util.Util.isWindowBackgroundDark(fragment.getActivity())) {
                int adjustedLastColor = ColorUtil.isColorLight(fragment.lastColor) ? ColorUtil.darkenColor(fragment.lastColor) : fragment.lastColor;
                int adjustedNewColor = ColorUtil.isColorLight(newColor) ? ColorUtil.darkenColor(newColor) : newColor;
                Animator subHeaderAnimator = ViewUtil.createTextColorTransition(fragment.viewBinding.playerQueueSubHeader, adjustedLastColor, adjustedNewColor);
                animatorSet.play(subHeaderAnimator);
            }

            animatorSet.setDuration(ViewUtil.PHONOGRAPH_ANIM_TIME);
            return animatorSet;
        }

        @Override
        public void animateColorChange(int newColor) {
            if (util.mddesign.util.Util.isWindowBackgroundDark(fragment.getActivity())) {
                fragment.viewBinding.playerQueueSubHeader.setTextColor(ThemeColor.textColorSecondary(fragment.requireActivity()));
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static class PortraitImpl extends BaseImpl {
        MediaEntryViewHolder currentSongViewHolder;
        Song currentSong = Song.EMPTY_SONG;

        public PortraitImpl(CardPlayerFragment fragment) {
            super(fragment);
        }

        @Override
        public void init() {
            currentSongViewHolder = new MediaEntryViewHolder(fragment.getView().findViewById(R.id.current_song));

            currentSongViewHolder.separator.setVisibility(View.VISIBLE);
            currentSongViewHolder.shortSeparator.setVisibility(View.GONE);
            currentSongViewHolder.image.setScaleType(ImageView.ScaleType.CENTER);
            currentSongViewHolder.image.setColorFilter(util.mddesign.util.Util.resolveColor(fragment.getActivity(), R.attr.iconColor, ThemeColor.textColorSecondary(fragment.getActivity())), PorterDuff.Mode.SRC_IN);
            currentSongViewHolder.image.setImageResource(R.drawable.ic_volume_up_white_24dp);
            currentSongViewHolder.itemView.setOnClickListener(v -> {
                // toggle the panel
                if (fragment.viewBinding.playerSlidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    fragment.viewBinding.playerSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                } else if (fragment.viewBinding.playerSlidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    fragment.viewBinding.playerSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            });
            currentSongViewHolder.menu.setOnClickListener(new SongMenuHelper.ClickMenuListener((AppCompatActivity) fragment.getActivity(), R.menu.menu_item_playing_queue_song) {
                @NonNull
                @Override
                public Song getSong() {
                    return currentSong;
                }

                @Override
                public boolean onMenuItemClick(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_remove_from_playing_queue:
                            MusicPlayerRemote.removeFromQueue(MusicPlayerRemote.getPosition());
                            return true;
                        case R.id.action_share:
                            SongShareDialog.create(getSong()).show(fragment.getFragmentManager(), "SONG_SHARE_DIALOG");
                            return true;
                    }
                    return super.onMenuItemClick(item);
                }
            });
        }

        @Override
        public void setUpPanelAndAlbumCoverHeight() {
            WidthFitSquareLayout albumCoverContainer = fragment.getView().findViewById(R.id.album_cover_container);

            final int availablePanelHeight = fragment.viewBinding.playerSlidingLayout.getHeight() - fragment.getView().findViewById(R.id.player_content).getHeight() + (int) ViewUtil.convertDpToPixel(8, fragment.getResources());
            final int minPanelHeight = (int) ViewUtil.convertDpToPixel(72 + 24, fragment.getResources());
            if (availablePanelHeight < minPanelHeight) {
                albumCoverContainer.getLayoutParams().height = albumCoverContainer.getHeight() - (minPanelHeight - availablePanelHeight);
                albumCoverContainer.forceSquare(false);
            }
            fragment.viewBinding.playerSlidingLayout.setPanelHeight(Math.max(minPanelHeight, availablePanelHeight));

            ((AbsSlidingMusicPanelActivity) fragment.getActivity()).setAntiDragView(fragment.viewBinding.playerSlidingLayout.findViewById(R.id.player_panel));
        }

        @Override
        public void updateCurrentSong(Song song) {
            currentSong = song;
            currentSongViewHolder.title.setText(song.title);
            currentSongViewHolder.text.setText(MusicUtil.getSongInfoString(song));
        }

        @Override
        public void animateColorChange(int newColor) {
            super.animateColorChange(newColor);

            fragment.viewBinding.playerSlidingLayout.setBackgroundColor(fragment.lastColor);

            createDefaultColorChangeAnimatorSet(newColor).start();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static class LandscapeImpl extends BaseImpl {
        public LandscapeImpl(CardPlayerFragment fragment) {
            super(fragment);
        }

        @Override
        public void init() {

        }

        @Override
        public void setUpPanelAndAlbumCoverHeight() {
            int panelHeight = fragment.viewBinding.playerSlidingLayout.getHeight() - fragment.playbackControlsFragment.getView().getHeight();
            fragment.viewBinding.playerSlidingLayout.setPanelHeight(panelHeight);

            ((AbsSlidingMusicPanelActivity) fragment.getActivity()).setAntiDragView(fragment.viewBinding.playerSlidingLayout.findViewById(R.id.player_panel));
        }

        @Override
        public void updateCurrentSong(Song song) {
            fragment.viewBinding.playerToolbar.setTitle(song.title);
            fragment.viewBinding.playerToolbar.setSubtitle(MusicUtil.getSongInfoString(song));
        }

        @Override
        public void animateColorChange(int newColor) {
            super.animateColorChange(newColor);

            fragment.viewBinding.playerSlidingLayout.setBackgroundColor(fragment.lastColor);

            AnimatorSet animatorSet = createDefaultColorChangeAnimatorSet(newColor);
            animatorSet.play(ViewUtil.createBackgroundColorTransition(fragment.viewBinding.playerToolbar, fragment.lastColor, newColor))
                    .with(ViewUtil.createBackgroundColorTransition(fragment.getView().findViewById(R.id.status_bar), ColorUtil.darkenColor(fragment.lastColor), ColorUtil.darkenColor(newColor)));
            animatorSet.start();
        }
    }
}
