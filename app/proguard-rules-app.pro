#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class lib.phonograph.preference.*PreferenceX {*;}

-keepclasseswithmembernames class lib.phonograph.activity.** {public <methods>; public <fields>;}


-keep class player.phonograph.ui.views.** {*;}

-keep class player.phonograph.model.** {public <fields>;}
-keepclasseswithmembernames class player.phonograph.repo.mediastore.playlist.** { <methods>;}

-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.activities.** extends android.app.Activity {public <methods>;}

-keepclasseswithmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.pages.AbsDisplayPage {<methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.explorer.*Explorer {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.pages.PlaylistPage {<methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.pages.**Page extends player.phonograph.ui.fragments.pages.AbsDisplayPage
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.player.** extends androidx.fragment.app.Fragment {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.HomeFragment {public <methods>;}

-keepclassmembernames class player.phonograph.ui.components.popup.ListOptionsPopup {public <methods>;}

-keepclassmembernames class player.phonograph.ui.dialogs.DeleteSongsDialog {public <methods>;}
-keepclassmembernames class player.phonograph.ui.dialogs.PathFilterDialog {public <methods>;}

-keepclasseswithmembernames class player.phonograph.ui.** extends androidx.lifecycle.ViewModel {public <methods>;}


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.repo.mediastore.internal.QueryKt {public <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.MusicService {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.notification.** {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.AudioPlayer {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.PlayerController {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.queue.QueueManager {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.queue.CurrentQueueState$Observer {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.util.** {public <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.adapter.DisplayAdapter {public <methods>; protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.adapter.SortableListAdapter {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.adapter.MultiSelectionController {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.search.SearchResultPageFragment {abstract <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.dialogs.HomeTabConfigDialog$PageTabConfigAdapter {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.dialogs.ImageSourceConfigDialog$ImageSourceConfigAdapter {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.HomeFragment$HomePagerAdapter {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.player.AlbumCoverPagerAdapter {public <methods>;}

-keep, allowoptimization, allowshrinking class player.phonograph.util.permissions.CheckKt { *; }
-keepnames,allowoptimization,allowshrinking class player.phonograph.util.permissions.** extends player.phonograph.util.permissions.Permission
-keepnames,allowoptimization,allowshrinking class player.phonograph.util.permissions.PermissionDelegate {
    public void grant(...);
}
-keepnames,allowoptimization,allowshrinking class player.phonograph.util.permissions.UIKt {
    public void notifyUser(...);
}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.theme.ThemeKt {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.theme.DrawableTintKt {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.zip.ZipUtil {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.util.file.* {public <methods>;}
-keep,allowoptimization,allowshrinking class player.phonograph.util.NavigationUtil { public <methods>; }
-keep,allowoptimization,allowshrinking class player.phonograph.mechanism.scanner.FileScanner {
    public *** listPaths(...);
}


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.Setting {
	public void set**(...);
	public *** get**();
}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.PrerequisiteSetting {
	public void set**(...);
	public *** get**();
}


-keepnames,allowoptimization,allowshrinking class player.phonograph.notification.* extends player.phonograph.notification.AbsNotificationImpl {public <methods>;}

-keep,allowoptimization,allowshrinking class player.phonograph.mechanism.backup.* extends player.phonograph.mechanism.backup.BackupItem {
 	*** getKey();
 	boolean import(java.io.InputStream,android.content.Context);
 	java.io.InputStream data(android.content.Context);
}

-keepclasseswithmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.migrate.Migration {
	public void doMigrate(android.content.Context);
}

-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.event.MediaStoreTracker$EventReceiver {
	public void onReceive(android.content.Context, android.content.Intent);
}

-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.event.MediaStoreTracker {
	public void dispatch();
}
-keep,allowoptimization,allowshrinking class player.phonograph.mechanism.lyrics.LyricsLoader {
	public *** loadLyrics(...);
}

-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.edit.CommonKt { public <methods>; }
-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.edit.SingleKt { public <methods>; }
-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.edit.MultipleKt { public <methods>; }
-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.ArtworkKt { public <methods>; }
-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.SongInfoReaderKt { public <methods>; }
-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.TagParserKt { public <methods>; }
-keepnames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.** extends player.phonograph.mechanism.tag.TagReader { public *** read(...); }
-keepclasseswithmembernames,allowshrinking class player.phonograph.mechanism.tag.* extends player.phonograph.mechanism.tag.EditAction { public <methods>; }


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.* extends player.phonograph.mechanism.IFavorite { public <methods>; }
-keep,allowoptimization,allowshrinking class player.phonograph.mechanism.UpdateKt { *** fetchVersionCatalog(...);}


-keepclassmembernames class lib.phonograph.misc.RestResult { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$Success { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$RemoteError { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$ParseError { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$NetworkError { <methods>; }