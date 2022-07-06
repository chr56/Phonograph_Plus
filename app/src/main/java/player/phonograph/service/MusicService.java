package player.phonograph.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import player.phonograph.App;
import player.phonograph.BuildConfig;
import player.phonograph.R;
import player.phonograph.appwidgets.AppWidgetBig;
import player.phonograph.appwidgets.AppWidgetCard;
import player.phonograph.appwidgets.AppWidgetClassic;
import player.phonograph.appwidgets.AppWidgetSmall;
import player.phonograph.glide.BlurTransformation;
import player.phonograph.glide.SongGlideRequest;
import player.phonograph.helper.ShuffleHelper;
import player.phonograph.helper.StopWatch;
import player.phonograph.misc.LyricsUpdateThread;
import player.phonograph.model.Song;
import player.phonograph.model.lyrics2.LrcLyrics;
import player.phonograph.model.playlist.Playlist;
import player.phonograph.notification.PlayingNotification;
import player.phonograph.notification.PlayingNotificationImpl;
import player.phonograph.notification.PlayingNotificationImpl24;
import player.phonograph.provider.HistoryStore;
import player.phonograph.provider.SongPlayCountStore;
import player.phonograph.service.playback.Playback;
import player.phonograph.service.queue.QueueChangeObserver;
import player.phonograph.service.queue.QueueManager;
import player.phonograph.service.queue.RepeatMode;
import player.phonograph.service.queue.ShuffleMode;
import player.phonograph.settings.Setting;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid), Andrew Neal
 */
public class MusicService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener, Playback.PlaybackCallbacks, LyricsUpdateThread.ProgressMillsUpdateCallback {

    public static final String PHONOGRAPH_PACKAGE_NAME = App.ACTUAL_PACKAGE_NAME;
    public static final String MUSIC_PACKAGE_NAME = "com.android.music";

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

    public static final int RELEASE_WAKELOCK = 0;
    public static final int TRACK_ENDED = 1;
    public static final int TRACK_WENT_TO_NEXT = 2;
    public static final int PLAY_SONG = 3;
    public static final int PREPARE_NEXT = 4;
    public static final int SET_POSITION = 5;
    private static final int FOCUS_CHANGE = 6;
    private static final int DUCK = 7;
    private static final int UNDUCK = 8;

    private final IBinder musicBind = new MusicBinder();

    public boolean pendingQuit = false;

    private AppWidgetBig appWidgetBig = AppWidgetBig.getInstance();
    private AppWidgetClassic appWidgetClassic = AppWidgetClassic.getInstance();
    private AppWidgetSmall appWidgetSmall = AppWidgetSmall.getInstance();
    private AppWidgetCard appWidgetCard = AppWidgetCard.getInstance();

    private Playback playback;
    private QueueManager queueManager;
    private QueueChangeObserver queueChangeObserver;

    private boolean pausedByTransientLossOfFocus;
    private PlayingNotification playingNotification;
    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private PowerManager.WakeLock wakeLock;
    private PlaybackHandler playerHandler;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            playerHandler.obtainMessage(FOCUS_CHANGE, focusChange, 0).sendToTarget();
        }
    };
    private HandlerThread musicPlayerHandlerThread;
    private SongPlayCountHelper songPlayCountHelper = new SongPlayCountHelper();
    private ThrottledSeekHandler throttledSeekHandler;
    private boolean becomingNoisyReceiverRegistered;
    private IntentFilter becomingNoisyReceiverIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                pause();
            }
        }
    };
    private ContentObserver mediaStoreObserver;
    private boolean notHandledMetaChangedForCurrentTrack;

    private Handler uiThreadHandler;

    private LyricsUpdateThread lyricsUpdateThread;


    private static String getTrackUri(@NonNull Song song) {
        return MusicUtil.INSTANCE.getSongFileUri(song.id).toString();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);

        musicPlayerHandlerThread = new HandlerThread("PlaybackHandler");
        musicPlayerHandlerThread.start();
        playerHandler = new PlaybackHandler(this, musicPlayerHandlerThread.getLooper());

        playback = new MultiPlayer(this);
        playback.setCallbacks(this);

        setupMediaSession();

        uiThreadHandler = new Handler();

        registerReceiver(widgetIntentReceiver, new IntentFilter(APP_WIDGET_UPDATE));

        initNotification();

        mediaStoreObserver = new MediaStoreObserver(playerHandler);
        throttledSeekHandler = new ThrottledSeekHandler(playerHandler);

        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);

        getContentResolver().registerContentObserver(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Albums.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Artists.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Genres.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver().registerContentObserver(MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI, true, mediaStoreObserver);


        Setting.Companion.getInstance().registerOnSharedPreferenceChangedListener(this);

        queueManager = App.getInstance().getQueueManager();

        // notify manually for first setting up queueManager
        sendChangeInternal(META_CHANGED);
        sendChangeInternal(QUEUE_CHANGED);

        restoreTrackPositionIfNecessary();

        mediaSession.setActive(true);

        sendBroadcast(new Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_CREATED"));

        lyricsUpdateThread = new LyricsUpdateThread(queueManager.getCurrentSong(), this);
        lyricsUpdateThread.start();

        queueChangeObserver = initQueueChangeObserver();
        queueManager.addObserver(queueChangeObserver);
    }

    private QueueChangeObserver initQueueChangeObserver() {
        return new QueueChangeObserver() {
            @Override
            public void onStateRestored() {
            }

            @Override
            public void onStateSaved() {
            }

            @Override
            public void onQueueCursorChanged(int newPosition) {
                // notifyChange(QUEUE_CHANGED);
            }

            @Override
            public void onQueueChanged(@NonNull List<? extends Song> newPlayingQueue, @NonNull List<? extends Song> newOriginalQueue) {
                notifyChange(QUEUE_CHANGED);
            }

            @Override
            public void onShuffleModeChanged(@NonNull ShuffleMode newMode) {
                notifyChange(SHUFFLE_MODE_CHANGED);
            }

            @Override
            public void onRepeatModeChanged(@NonNull RepeatMode newMode) {
                prepareNext();
                notifyChange(REPEAT_MODE_CHANGED);
            }
        };
    }

    private AudioManager getAudioManager() {
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        return audioManager;
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

        // noinspection deprecation
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
        ); // fixme remove deprecation

        mediaSession.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                restoreTrackPositionIfNecessary();
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
                        Playlist playlist = intent.getParcelableExtra(INTENT_EXTRA_PLAYLIST);
                        ShuffleMode shuffleMode =
                                ShuffleMode.Companion.deserialize(
                                        intent.getIntExtra(INTENT_EXTRA_SHUFFLE_MODE, 0)
                                );
                        if (playlist != null) {
                            List<Song> playlistSongs = playlist.getSongs(App.getInstance());
                            if (!playlistSongs.isEmpty()) {
                                if (queueManager.getShuffleMode() == ShuffleMode.SHUFFLE) {
                                    int startPosition = 0;
                                    if (!playlistSongs.isEmpty()) {
                                        startPosition = new Random().nextInt(playlistSongs.size());
                                    }
                                    openQueue(playlistSongs, startPosition, true);
                                    setShuffleMode(shuffleMode);
                                } else {
                                    openQueue(playlistSongs, 0, true);
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), R.string.playlist_is_empty, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.playlist_is_empty, Toast.LENGTH_LONG).show();
                        }
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
        unregisterReceiver(widgetIntentReceiver);
        if (becomingNoisyReceiverRegistered) {
            unregisterReceiver(becomingNoisyReceiver);
            becomingNoisyReceiverRegistered = false;
        }
        mediaSession.setActive(false);
        quit();
        releaseResources();
        getContentResolver().unregisterContentObserver(mediaStoreObserver);
        Setting.Companion.getInstance().unregisterOnSharedPreferenceChangedListener(this);
        wakeLock.release();

        queueManager.removeObserver(queueChangeObserver);

        sendBroadcast(new Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_DESTROYED"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public int getProgressTimeMills() {
        return getSongProgressMillis();
    }

    @Override
    public boolean isRunning() {
        return isPlaying();
    }


    private void savePositionInTrack() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(SAVED_POSITION_IN_TRACK, getSongProgressMillis()).apply();
    }

    public void saveState() {
        queueManager.postMessage(QueueManager.MSG_SAVE_QUEUE);
        queueManager.postMessage(QueueManager.MSG_SAVE_CURSOR);
        savePositionInTrack();
    }


    private Boolean queuesRestored = false;

    private synchronized void restoreTrackPositionIfNecessary() {
        if (!queuesRestored) {
            int restoredPositionInTrack = PreferenceManager.getDefaultSharedPreferences(this).getInt(SAVED_POSITION_IN_TRACK, -1);
            openCurrent();
            prepareNext();
            if (restoredPositionInTrack > 0) seek(restoredPositionInTrack);
            notHandledMetaChangedForCurrentTrack = true;//todo
            sendChangeInternal(META_CHANGED);
            queuesRestored = true;
        }
    }

    private void quit() {
        pause();
        isQuit = true;
        playingNotification.stop();

        closeAudioEffectSession();
        getAudioManager().abandonAudioFocus(audioFocusListener);
        stopSelf();
    }

    private void releaseResources() {
        playerHandler.removeCallbacksAndMessages(null);
        musicPlayerHandlerThread.quitSafely();
        lyricsUpdateThread.setCurrentSong(null);
        lyricsUpdateThread.quit();
        lyricsUpdateThread = null;
        playback.release();
        playback = null;
        mediaSession.release();
    }

    public boolean isPlaying() {
        return playback != null && playback.isPlaying();
    }

    private boolean isQuit = false;

    public boolean isIdle() {
        return isQuit;
    }

    public void playNextSong(boolean force) {
        if (force) {
            int nextPosition = queueManager.getCurrentSongPosition() + 1;
            if (nextPosition >= queueManager.getPlayingQueue().size()) return;
            playSongAt(nextPosition);
        } else {
            playSongAt(queueManager.getNextSongPosition());
        }
    }

    private boolean openTrackAndPrepareNextAt(int position) {
        synchronized (this) {
            queueManager.setQueueCursor(position);
            boolean prepared = openCurrent();
            if (prepared) prepareNextImpl();
            notifyChange(META_CHANGED);
            notHandledMetaChangedForCurrentTrack = false;
            return prepared;
        }
    }

    private boolean openCurrent() {
        synchronized (this) {
            try {
                return playback.setDataSource(getTrackUri(queueManager.getCurrentSong()));
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void prepareNext() {
        playerHandler.removeMessages(PREPARE_NEXT);
        playerHandler.obtainMessage(PREPARE_NEXT).sendToTarget();
    }

    private boolean prepareNextImpl() {
        synchronized (this) {
            try {
                int nextPosition = queueManager.getNextSongPosition();
                playback.setNextDataSource(getTrackUri(queueManager.getSongAt(nextPosition)));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void closeAudioEffectSession() {
        final Intent audioEffectsIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playback.getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
    }

    private boolean requestFocus() {
        return (getAudioManager().requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
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
                            metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, copy(resource));
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

    private static Bitmap copy(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.RGB_565;
        }
        try {
            return bitmap.copy(config, false);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    public void runOnUiThread(Runnable runnable) {
        uiThreadHandler.post(runnable);
    }

    public void setRepeatMode(final RepeatMode repeatMode) {
        queueManager.switchRepeatMode(repeatMode);
        prepareNext();
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
        // handle this on the handlers thread to avoid blocking the ui thread
        playerHandler.removeMessages(PLAY_SONG);
        playerHandler.obtainMessage(PLAY_SONG, position, 0).sendToTarget();

        broadcastStopLyric(); // clear lyrics on switching

        lyricsUpdateThread.setCurrentSong(queueManager.getSongAt(position));
    }

    public void setPosition(final int position) {
        // handle this on the handlers thread to avoid blocking the ui thread
        playerHandler.removeMessages(SET_POSITION);
        playerHandler.obtainMessage(SET_POSITION, position, 0).sendToTarget();

        lyricsUpdateThread.setCurrentSong(queueManager.getSongAt(position));
    }

    private void playSongAtImpl(int position) {
        if (openTrackAndPrepareNextAt(position)) {
            play();
        } else {
            Toast.makeText(this, getResources().getString(R.string.unplayable_file), Toast.LENGTH_SHORT).show();
            // todo add a preference to control this behavior
            if (
                    (position != queueManager.getPlayingQueue().size() - 1)
                            && (queueManager.getRepeatMode() != RepeatMode.REPEAT_SINGLE_SONG)
            ) {
                playNextSong(true);
            }
        }
    }

    public void pause() {
        pausedByTransientLossOfFocus = false;
        if (playback.isPlaying()) {
            playback.pause();
            notifyChange(PLAY_STATE_CHANGED);
            broadcastStopLyric(); // clear lyrics on pause/stop
        }
    }

    public void play() {
        synchronized (this) {
            if (requestFocus()) {
                if (!playback.isPlaying()) {
                    if (!playback.isInitialized()) {
                        playSongAt(queueManager.getCurrentSongPosition());
                    } else {
                        playback.start();
                        isQuit = false;
                        if (!becomingNoisyReceiverRegistered) {
                            registerReceiver(becomingNoisyReceiver, becomingNoisyReceiverIntentFilter);
                            becomingNoisyReceiverRegistered = true;
                        }
                        if (notHandledMetaChangedForCurrentTrack) {
                            handleChangeInternal(META_CHANGED);
                            notHandledMetaChangedForCurrentTrack = false;
                        }
                        notifyChange(PLAY_STATE_CHANGED);

                        // fixes a bug where the volume would stay ducked because the AudioManager.AUDIOFOCUS_GAIN event is not sent
                        playerHandler.removeMessages(DUCK);
                        playerHandler.sendEmptyMessage(UNDUCK);

                        broadcastStopLyric(); // clear lyrics on staring

                        lyricsUpdateThread.setCurrentSong(queueManager.getSongAt(queueManager.getCurrentSongPosition()));
                    }
                }
            } else {
                Toast.makeText(this, getResources().getString(R.string.audio_focus_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void playPreviousSong(boolean force) {
        if (force) {
            int previousPosition = queueManager.getCurrentSongPosition() - 1;
            if (previousPosition < 0) return;
            playSongAt(previousPosition);
        } else
            playSongAt(queueManager.getPreviousSongPosition());
    }

    public void back(boolean force) {
        if (getSongProgressMillis() > 5000) {
            seek(0);
        } else {
            playPreviousSong(force);
        }
    }


    public int getSongProgressMillis() {
        return playback.position();
    }

    public int getSongDurationMillis() {
        return playback.duration();
    }


    public int seek(int millis) {
        synchronized (this) {
            try {
                int newPosition = playback.seek(millis);
                throttledSeekHandler.notifySeek();
                return newPosition;
            } catch (Exception e) {
                return -1;
            }
        }
    }


    public void setShuffleMode(final ShuffleMode shuffleMode) {
        queueManager.switchShuffleMode(shuffleMode); //todo
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
        appWidgetBig.notifyChange(this, what);
        appWidgetClassic.notifyChange(this, what);
        appWidgetSmall.notifyChange(this, what);
        appWidgetCard.notifyChange(this, what);
    }

    private static final long MEDIA_SESSION_ACTIONS = PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SEEK_TO;

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
                    prepareNext();
                } else {
                    isQuit = true;
                    playingNotification.stop();
                }
                break;
        }
    }

    public int getAudioSessionId() {
        return playback.getAudioSessionId();
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    public void releaseWakeLock() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void acquireWakeLock(long milli) {
        wakeLock.acquire(milli);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Setting.GAPLESS_PLAYBACK:
                if (sharedPreferences.getBoolean(key, false)) {
                    prepareNext();
                } else {
                    playback.setNextDataSource(null);
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

    @Override
    public void onTrackWentToNext() {
        playerHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
    }

    @Override
    public void onTrackEnded() {
        acquireWakeLock(30000);
        broadcastStopLyric(); // clear lyrics on ending
        playerHandler.sendEmptyMessage(TRACK_ENDED);
    }

    private static final class PlaybackHandler extends Handler {
        @NonNull
        private final WeakReference<MusicService> mService;
        private float currentDuckVolume = 1.0f;

        public PlaybackHandler(final MusicService service, @NonNull final Looper looper) {
            super(looper);
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            final MusicService service = mService.get();
            if (service == null) {
                return;
            }

            switch (msg.what) {
                case DUCK:
                    if (Setting.instance().getAudioDucking()) {
                        currentDuckVolume -= .05f;
                        if (currentDuckVolume > .2f) {
                            sendEmptyMessageDelayed(DUCK, 10);
                        } else {
                            currentDuckVolume = .2f;
                        }
                    } else {
                        currentDuckVolume = 1f;
                    }
                    service.playback.setVolume(currentDuckVolume);
                    break;

                case UNDUCK:
                    if (Setting.instance().getAudioDucking()) {
                        currentDuckVolume += .03f;
                        if (currentDuckVolume < 1f) {
                            sendEmptyMessageDelayed(UNDUCK, 10);
                        } else {
                            currentDuckVolume = 1f;
                        }
                    } else {
                        currentDuckVolume = 1f;
                    }
                    service.playback.setVolume(currentDuckVolume);
                    break;

                case TRACK_WENT_TO_NEXT:
                    if (service.pendingQuit || service.queueManager.getShuffleMode() == ShuffleMode.NONE && service.queueManager.isLastTrack()) {
                        service.pause();
                        service.seek(0);
                        if (service.pendingQuit) {
                            service.pendingQuit = false;
                            service.quit();
                            break;
                        }
                    } else {
                        service.queueManager.setQueueCursor(
                                service.queueManager.getNextSongPosition()
                        );
                        service.prepareNextImpl();
                        service.notifyChange(META_CHANGED);
                    }
                    break;

                case TRACK_ENDED:
                    // if there is a timer finished, don't continue
                    if (service.pendingQuit ||
                            service.queueManager.getShuffleMode() == ShuffleMode.NONE && service.queueManager.isLastTrack()) {
                        service.notifyChange(PLAY_STATE_CHANGED);
                        service.seek(0);
                        if (service.pendingQuit) {
                            service.pendingQuit = false;
                            service.quit();
                            break;
                        }
                    } else {
                        service.playNextSong(false);
                    }
                    sendEmptyMessage(RELEASE_WAKELOCK);
                    break;

                case RELEASE_WAKELOCK:
                    service.releaseWakeLock();
                    break;

                case PLAY_SONG:
                    service.playSongAtImpl(msg.arg1);
                    break;

                case SET_POSITION:
                    service.openTrackAndPrepareNextAt(msg.arg1);
                    service.notifyChange(PLAY_STATE_CHANGED);
                    break;

                case PREPARE_NEXT:
                    service.prepareNextImpl();
                    break;

                case FOCUS_CHANGE:
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (!service.isPlaying() && service.pausedByTransientLossOfFocus) {
                                service.play();
                                service.pausedByTransientLossOfFocus = false;
                            }
                            removeMessages(DUCK);
                            sendEmptyMessage(UNDUCK);
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS:
                            // Lost focus for an unbounded amount of time: stop playback and release media playback
                            service.pause();
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Lost focus for a short time, but we have to stop
                            // playback. We don't release the media playback because playback
                            // is likely to resume
                            boolean wasPlaying = service.isPlaying();
                            service.pause();
                            service.pausedByTransientLossOfFocus = wasPlaying;
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Lost focus for a short time, but it's ok to keep playing
                            // at an attenuated level
                            removeMessages(UNDUCK);
                            sendEmptyMessage(DUCK);
                            break;
                    }
                    break;
            }
        }
    }

    public class MusicBinder extends Binder {
        @NonNull
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private final BroadcastReceiver widgetIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String command = intent.getStringExtra(EXTRA_APP_WIDGET_NAME);

            final int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            switch (command) {
                case AppWidgetClassic.NAME: {
                    appWidgetClassic.performUpdate(MusicService.this, ids);
                    break;
                }
                case AppWidgetSmall.NAME: {
                    appWidgetSmall.performUpdate(MusicService.this, ids);
                    break;
                }
                case AppWidgetBig.NAME: {
                    appWidgetBig.performUpdate(MusicService.this, ids);
                    break;
                }
                case AppWidgetCard.NAME: {
                    appWidgetCard.performUpdate(MusicService.this, ids);
                    break;
                }
            }
        }
    };

    private class MediaStoreObserver extends ContentObserver implements Runnable {
        // milliseconds to delay before calling refresh to aggregate events
        private static final long REFRESH_DELAY = 500;
        private Handler mHandler;

        public MediaStoreObserver(Handler handler) {
            super(handler);
            mHandler = handler;
        }

        @Override
        public void onChange(boolean selfChange) {
            // if a change is detected, remove any scheduled callback
            // then post a new one. This is intended to prevent closely
            // spaced events from generating multiple refresh calls
            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, REFRESH_DELAY);
        }

        @Override
        public void run() {
            // actually call refresh when the delayed callback fires
            // do not send a sticky broadcast here
            handleAndSendChangeInternal(MEDIA_STORE_CHANGED);
        }
    }

    private class ThrottledSeekHandler implements Runnable {
        // milliseconds to throttle before calling run() to aggregate events
        private static final long THROTTLE = 500;
        private Handler mHandler;

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

    private static class SongPlayCountHelper {
        public static final String TAG = SongPlayCountHelper.class.getSimpleName();

        private StopWatch stopWatch = new StopWatch();
        private Song song = Song.EMPTY_SONG;

        public Song getSong() {
            return song;
        }

        boolean shouldBumpPlayCount() {
            return song.duration * 0.5d < stopWatch.getElapsedTime();
        }

        void notifySongChanged(Song song) {
            synchronized (this) {
                stopWatch.reset();
                this.song = song;
            }
        }

        void notifyPlayStateChanged(boolean isPlaying) {
            synchronized (this) {
                if (isPlaying) {
                    stopWatch.start();
                } else {
                    stopWatch.pause();
                }
            }
        }
    }

    /**
     * broadcast for "MIUI StatusBar Lyrics" Xposed module
     */
    private void broadcastStopLyric() {
        App.getInstance().getLyricsService().stopLyric();
    }

    public void replaceLyrics(LrcLyrics lyrics) {
        if (lyrics != null) {
            lyricsUpdateThread.forceReplaceLyrics(lyrics);
        } else {
            lyricsUpdateThread.setCurrentSong(null);
        }
    }
}
