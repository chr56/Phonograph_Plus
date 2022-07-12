package player.phonograph.service;

import static player.phonograph.service.player.PlayerStateObserverKt.MSG_NOW_PLAYING_CHANGED;
import static player.phonograph.service.player.PlayerStateObserverKt.MSG_NO_MORE_SONGS;
import static player.phonograph.service.player.PlayerStateObserverKt.MSG_PLAYER_STOPPED;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;

import kotlin.Unit;
import player.phonograph.App;
import player.phonograph.BuildConfig;
import player.phonograph.glide.BlurTransformation;
import player.phonograph.glide.SongGlideRequest;
import player.phonograph.model.Song;
import player.phonograph.model.lyrics2.LrcLyrics;
import player.phonograph.notification.PlayingNotification;
import player.phonograph.notification.PlayingNotificationImpl;
import player.phonograph.notification.PlayingNotificationImpl24;
import player.phonograph.provider.HistoryStore;
import player.phonograph.provider.SongPlayCountStore;
import player.phonograph.service.player.PlayerController;
import player.phonograph.service.player.PlayerState;
import player.phonograph.service.player.PlayerStateObserver;
import player.phonograph.service.queue.QueueChangeObserver;
import player.phonograph.service.queue.QueueManager;
import player.phonograph.service.queue.RepeatMode;
import player.phonograph.service.queue.ShuffleMode;
import player.phonograph.service.util.SongPlayCountHelper;
import player.phonograph.settings.Setting;
import player.phonograph.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid), Andrew Neal
 */
public class MusicService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PHONOGRAPH_PACKAGE_NAME = App.ACTUAL_PACKAGE_NAME;

    public static final String ACTION_TOGGLE_PAUSE = PHONOGRAPH_PACKAGE_NAME + ".togglepause";
    public static final String ACTION_PLAY = PHONOGRAPH_PACKAGE_NAME + ".play";
    public static final String ACTION_PLAY_PLAYLIST = PHONOGRAPH_PACKAGE_NAME + ".play.playlist";
    public static final String ACTION_PAUSE = PHONOGRAPH_PACKAGE_NAME + ".pause";
    public static final String ACTION_STOP = PHONOGRAPH_PACKAGE_NAME + ".stop";
    public static final String ACTION_SKIP = PHONOGRAPH_PACKAGE_NAME + ".skip";
    public static final String ACTION_REWIND = PHONOGRAPH_PACKAGE_NAME + ".rewind";
    public static final String ACTION_QUIT = PHONOGRAPH_PACKAGE_NAME + ".quitservice";
    public static final String ACTION_PENDING_QUIT = PHONOGRAPH_PACKAGE_NAME + ".pendingquitservice";
    public static final String INTENT_EXTRA_PLAYLIST = PHONOGRAPH_PACKAGE_NAME + "intentextra.playlist";
    public static final String INTENT_EXTRA_SHUFFLE_MODE = PHONOGRAPH_PACKAGE_NAME + ".intentextra.shufflemode";

    public static final String APP_WIDGET_UPDATE = PHONOGRAPH_PACKAGE_NAME + ".appwidgetupdate";
    public static final String EXTRA_APP_WIDGET_NAME = PHONOGRAPH_PACKAGE_NAME + "app_widget_name";

    // do not change these three strings as it will break support with other apps (e.g. last.fm scrobbling)
    public static final String META_CHANGED = PHONOGRAPH_PACKAGE_NAME + ".metachanged";
    public static final String QUEUE_CHANGED = PHONOGRAPH_PACKAGE_NAME + ".queuechanged"; // todo
    public static final String PLAY_STATE_CHANGED = PHONOGRAPH_PACKAGE_NAME + ".playstatechanged";

    public static final String REPEAT_MODE_CHANGED = PHONOGRAPH_PACKAGE_NAME + ".repeatmodechanged";
    public static final String SHUFFLE_MODE_CHANGED = PHONOGRAPH_PACKAGE_NAME + ".shufflemodechanged";

    public static final String MEDIA_STORE_CHANGED = PHONOGRAPH_PACKAGE_NAME + ".mediastorechanged";

    public static final String SAVED_POSITION_IN_TRACK = "POSITION_IN_TRACK";
    private static final long MEDIA_SESSION_ACTIONS = PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SEEK_TO;
    private final IBinder musicBind = new MusicBinder();
    private final SongPlayCountHelper songPlayCountHelper = new SongPlayCountHelper();
    public boolean pendingQuit = false;
    private MusicServiceKt musicServiceKt;
    private QueueManager queueManager;
    private QueueChangeObserver queueChangeObserver;
    private PlayerController controller;
    private PlayerStateObserver playerStateObserver;
    private PlayingNotification playingNotification;
    private MediaSessionCompat mediaSession;
    private ThrottledSeekHandler throttledSeekHandler;
    private Handler uiThreadHandler;
    private boolean isQuit = false; // todo sleeptimer

    static void log(@NonNull String msg) {
        if (BuildConfig.DEBUG)
            Log.i("MusicServiceDebug", msg);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        queueManager = App.getInstance().getQueueManager();
        controller = new PlayerController(this);

        setupMediaSession();

        uiThreadHandler = new Handler(Looper.getMainLooper());

        musicServiceKt = new MusicServiceKt(this);
        registerReceiver(musicServiceKt.widgetIntentReceiver, new IntentFilter(APP_WIDGET_UPDATE));

        initNotification();

        // todo use other handler
        musicServiceKt.setUpMediaStoreObserver(this, controller.getHandler(), (String s) -> {
            handleAndSendChangeInternal(s);
            return Unit.INSTANCE;
        });
        throttledSeekHandler = new ThrottledSeekHandler(controller.getHandler());

        Setting.instance().registerOnSharedPreferenceChangedListener(this);


        // notify manually for first setting up queueManager
        sendChangeInternal(META_CHANGED);
        sendChangeInternal(QUEUE_CHANGED);

        controller.restoreIfNecessary();

        mediaSession.setActive(true);

        queueChangeObserver = initQueueChangeObserver();
        queueManager.addObserver(queueChangeObserver);

        playerStateObserver = initPlayerStateObserver();
        controller.addObserver(playerStateObserver);

        sendBroadcast(new Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_CREATED"));
    }

    private QueueChangeObserver initQueueChangeObserver() {
        return new QueueChangeObserver() {
            @Override
            public void onStateSaved() {
            }

            @Override
            public void onStateRestored() {
            }

            @Override
            public void onQueueCursorChanged(int newPosition) {
                notifyChange(META_CHANGED);
            }

            @Override
            public void onQueueChanged(@NonNull List<? extends Song> newPlayingQueue, @NonNull List<? extends Song> newOriginalQueue) {
                notifyChange(QUEUE_CHANGED);
                notifyChange(META_CHANGED);
            }

            @Override
            public void onShuffleModeChanged(@NonNull ShuffleMode newMode) {
                notifyChange(SHUFFLE_MODE_CHANGED);
            }

            @Override
            public void onRepeatModeChanged(@NonNull RepeatMode newMode) {
                controller.getHandler().removeMessages(PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER);
                controller.getHandler().sendEmptyMessage(PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER);
                notifyChange(REPEAT_MODE_CHANGED);
            }
        };
    }

    private PlayerStateObserver initPlayerStateObserver() {
        return new PlayerStateObserver() {
            @Override
            public void onPlayerStateChanged(@NonNull PlayerState oldState, @NonNull PlayerState newState) {
                notifyChange(PLAY_STATE_CHANGED);
            }

            @Override
            public void onReceivingMessage(int msg) {
                switch (msg) {
                    case MSG_NOW_PLAYING_CHANGED:
                        notifyChange(META_CHANGED);
                        break;
                    case MSG_NO_MORE_SONGS:
                        //todo
                        break;
                    case MSG_PLAYER_STOPPED:
                        quit();
                        break;
                }
            }
        };
    }

    private void setupMediaSession() {
        ComponentName mediaButtonReceiverComponentName = new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mediaButtonReceiverComponentName);

        PendingIntent mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE);

        mediaSession = new MediaSessionCompat(this, BuildConfig.APPLICATION_ID, mediaButtonReceiverComponentName, mediaButtonReceiverPendingIntent);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNextSong(true);
            }

            @Override
            public void onSkipToPrevious() {
                back(true);
            }

            @Override
            public void onStop() {
                quit();
            }

            @Override
            public void onSeekTo(long pos) {
                seek((int) pos);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                return MediaButtonIntentReceiver.Companion.handleIntent(MusicService.this, mediaButtonEvent);
            }
        });

        // noinspection deprecation // fixme remove deprecation
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
        );


        mediaSession.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                controller.restoreIfNecessary();
                String action = intent.getAction();
                switch (action) {
                    case ACTION_TOGGLE_PAUSE:
                        if (isPlaying()) {
                            pause();
                        } else {
                            play();
                        }
                        break;
                    case ACTION_PAUSE:
                        pause();
                        break;
                    case ACTION_PLAY:
                        play();
                        break;
                    case ACTION_PLAY_PLAYLIST:
                        MusicServiceKt.parsePlaylistAndPlay(intent, this);
                        break;
                    case ACTION_REWIND:
                        back(true);
                        break;
                    case ACTION_SKIP:
                        playNextSong(true);
                        break;
                    case ACTION_STOP:
                    case ACTION_QUIT:
                        pendingQuit = false;
                        quit();
                        break;
                    case ACTION_PENDING_QUIT:
                        pendingQuit = true;
                        break;
                }
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(musicServiceKt.widgetIntentReceiver);

        mediaSession.setActive(false);
        quit();
        mediaSession.release();
        musicServiceKt.unregisterMediaStoreObserver(this);
        Setting.Companion.getInstance().unregisterOnSharedPreferenceChangedListener(this);

        controller.destroy();
        controller.removeObserver(playerStateObserver);
        queueManager.removeObserver(queueChangeObserver);

        sendBroadcast(new Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_DESTROYED"));
    }

    //todo
    private void quit() {
        controller.pause();
        pause();
        isQuit = true;
        playingNotification.stop();

        closeAudioEffectSession();
        controller.destroy();
        stopSelf();
    }

    public boolean isPlaying() {
        return controller.isPlaying();
    }

    public boolean isIdle() {
        return isQuit;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    private void savePositionInTrack() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(SAVED_POSITION_IN_TRACK, getSongProgressMillis()).apply();
    }

    public void saveState() {
        queueManager.postMessage(QueueManager.MSG_SAVE_QUEUE);
        queueManager.postMessage(QueueManager.MSG_SAVE_CURSOR);
        savePositionInTrack();
    }

    public void playNextSong(boolean force) {
        controller.jumpForward(force);
    }

    //todo
    private void closeAudioEffectSession() {
        final Intent audioEffectsIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, controller.getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
    }

    public void initNotification() {
        if (!Setting.Companion.getInstance().getClassicNotification() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playingNotification = new PlayingNotificationImpl24(this);
        } else {
            playingNotification = new PlayingNotificationImpl(this);
        }
    }

    public void updateNotification() {
        Song song = queueManager.getCurrentSong();
        if (playingNotification != null && song.id != -1) {
            playingNotification.setMetaData(new PlayingNotification.SongMetaData(song));
        }
    }

    private void updateMediaSessionPlaybackState() {
        mediaSession.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setActions(MEDIA_SESSION_ACTIONS)
                        .setState(isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                                getSongProgressMillis(), 1)
                        .build());
    }

    private void updateMediaSessionMetaData() {
        final Song song = queueManager.getCurrentSong();

        if (song.id == -1) {
            mediaSession.setMetadata(null);
            return;
        }

        final MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, queueManager.getCurrentSongPosition() + 1)
                .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.year)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, queueManager.getPlayingQueue().size());


        if (Setting.Companion.getInstance().getAlbumArtOnLockscreen()) {
            final Point screenSize = Util.getScreenSize(MusicService.this);
            final RequestBuilder<Bitmap> request =
                    SongGlideRequest.Builder.from(Glide.with(MusicService.this), song)
                            .checkIgnoreMediaStore(MusicService.this)
                            .asBitmap().build();
            if (Setting.Companion.getInstance().getBlurredAlbumArt()) {
                request.transform(new BlurTransformation.Builder(MusicService.this).build());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    request.into(new CustomTarget<Bitmap>(screenSize.x, screenSize.y) {
                        @Override
                        public void onLoadFailed(Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            mediaSession.setMetadata(metaData.build());
                        }

                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, MusicServiceKt.copy(resource));
                            mediaSession.setMetadata(metaData.build());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            mediaSession.setMetadata(metaData.build()); // todo check leakage
                        }
                    });
                }
            });
        } else {
            mediaSession.setMetadata(metaData.build());
        }
    }

    public void runOnUiThread(Runnable runnable) {
        uiThreadHandler.post(runnable);
    }

    public void openQueue(@Nullable final List<Song> playingQueue, final int startPosition, final boolean startPlaying) {
        if (playingQueue != null && !playingQueue.isEmpty() && startPosition >= 0 && startPosition < playingQueue.size()) {
            queueManager.swapQueue(playingQueue, startPosition);
            if (startPlaying) {
                playSongAt(queueManager.getCurrentSongPosition());
            }
            notifyChange(QUEUE_CHANGED);
        }
    }

    public void playSongAt(final int position) {
        controller.playAt(position);
    }

    public void pause() {
        controller.pause();
    }

    public void play() {
        controller.play();
    }

    public void playPreviousSong(boolean force) {
        controller.jumpBackward(force);
    }

    public void back(boolean force) {
        if (getSongProgressMillis() > 5000) {
            seek(0);
        } else {
            playPreviousSong(force);
        }
    }

    public int getSongProgressMillis() {
        return controller.getSongProgressMillis();
    }

    public int getSongDurationMillis() {
        return controller.getSongDurationMillis();
    }

    // todo check
    public int seek(int millis) {
        synchronized (this) {
            try {
                int newPosition = controller.seekTo(millis);
                throttledSeekHandler.notifySeek();
                return newPosition;
            } catch (Exception e) {
                return -1;
            }
        }
    }

    private void notifyChange(@NonNull final String what) {
        handleAndSendChangeInternal(what);
        sendPublicIntent(what);
    }

    private void handleAndSendChangeInternal(@NonNull final String what) {
        handleChangeInternal(what);
        sendChangeInternal(what);
    }

    // to let other apps know whats playing. i.E. last.fm (scrobbling) or musixmatch
    private void sendPublicIntent(@NonNull final String what) {
        MusicServiceKt.sendPublicIntent(this, what);
    }

    private void sendChangeInternal(final String what) {
        sendBroadcast(new Intent(what));
        musicServiceKt.appWidgetBig.notifyChange(this, what);
        musicServiceKt.appWidgetClassic.notifyChange(this, what);
        musicServiceKt.appWidgetSmall.notifyChange(this, what);
        musicServiceKt.appWidgetCard.notifyChange(this, what);
    }

    private void handleChangeInternal(@NonNull final String what) {
        switch (what) {
            case PLAY_STATE_CHANGED:
                updateNotification();
                updateMediaSessionPlaybackState();
                final boolean isPlaying = isPlaying();
                if (!isPlaying && getSongProgressMillis() > 0) {
                    savePositionInTrack();
                }
                songPlayCountHelper.notifyPlayStateChanged(isPlaying);
                break;
            case META_CHANGED:
                updateNotification();
                updateMediaSessionMetaData();
                queueManager.postMessage(QueueManager.MSG_SAVE_CURSOR);
                savePositionInTrack();
                final Song currentSong = queueManager.getCurrentSong();
                HistoryStore.Companion.getInstance(this).addSongId(currentSong.id);
                if (songPlayCountHelper.shouldBumpPlayCount()) {
                    SongPlayCountStore.Companion.getInstance(this).bumpPlayCount(songPlayCountHelper.getSong().id);
                }
                songPlayCountHelper.notifySongChanged(currentSong);
                break;
            case QUEUE_CHANGED:
                updateMediaSessionMetaData(); // because playing queue size might have changed
                saveState();
                if (queueManager.getPlayingQueue().size() > 0) {
                    isQuit = false;
                    controller.getHandler().removeMessages(PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER);
                    controller.getHandler().sendEmptyMessage(PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER);
                } else {
                    isQuit = true;
                    playingNotification.stop();
                }
                break;
        }
    }

    public int getAudioSessionId() {
        return controller.getAudioSessionId();
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Setting.GAPLESS_PLAYBACK:
                //todo
                if (sharedPreferences.getBoolean(key, false)) {
                    controller.getHandler().removeMessages(PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER);
                    controller.getHandler().sendEmptyMessage(PlayerController.ControllerHandler.RE_PREPARE_NEXT_PLAYER);
                } else {
                    controller.getHandler().removeMessages(PlayerController.ControllerHandler.CLEAN_NEXT_PLAYER);
                    controller.getHandler().sendEmptyMessage(PlayerController.ControllerHandler.CLEAN_NEXT_PLAYER);
                }
                break;
            case Setting.ALBUM_ART_ON_LOCKSCREEN:
            case Setting.BLURRED_ALBUM_ART:
                updateMediaSessionMetaData();
                break;
            case Setting.COLORED_NOTIFICATION:
                updateNotification();
                break;
            case Setting.CLASSIC_NOTIFICATION:
                initNotification();
                updateNotification();
                break;
        }
    }

    public void replaceLyrics(LrcLyrics lyrics) {
        controller.replaceLyrics(lyrics);
    }

    public class MusicBinder extends Binder {
        @NonNull
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private class ThrottledSeekHandler implements Runnable {
        // milliseconds to throttle before calling run() to aggregate events
        private static final long THROTTLE = 500;
        private final Handler mHandler;

        public ThrottledSeekHandler(Handler handler) {
            mHandler = handler;
        }

        public void notifySeek() {
            updateMediaSessionMetaData();
            updateMediaSessionPlaybackState();
            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, THROTTLE);

        }

        @Override
        public void run() {
            savePositionInTrack();
            sendPublicIntent(PLAY_STATE_CHANGED); // for musixmatch synced lyrics
        }
    }
}