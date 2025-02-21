#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class player.phonograph.ui.views.** {*;}
-keep class player.phonograph.model.** {public <fields>;}

-keepclassmembernames class lib.phonograph.misc.RestResult { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$Success { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$RemoteError { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$ParseError { <methods>; }
-keepclassmembernames class lib.phonograph.misc.RestResult$NetworkError { <methods>; }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.adapter.DisplayAdapter { protected <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.adapter.DisplayAdapter$DisplayViewHolder { protected <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.basis.** extends android.app.Activity { protected <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.LauncherActivity { void goto*(); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.MigrationActivity { void *Impl();}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.PhonographIntroActivity$* { private <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.StarterActivity { private <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.explorer.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.main.pages.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.main.pages.adapter.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.panel.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.player.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.** extends androidx.lifecycle.ViewModel {public <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.repo.mediastore.internal.QueryKt { public query*(...); }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.MusicService { public <methods>;public <fields>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.notification.** { public <methods>;public <fields>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.VanillaAudioPlayer { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.PlayerController { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.PlayerController$VanillaAudioPlayerControllerImpl { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking interface player.phonograph.service.** { <methods>; }


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.file.UriKt { public void select*(...); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.file.WriteKt { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.NavigationUtil { public <methods>; }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.lyrics.LyricsLoader { public *** loadLyrics(...); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.migrate.Migration { public void doMigrate(android.content.Context); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.backup.* extends player.phonograph.mechanism.backup.BackupItem {
 	*** getKey();
 	boolean import(java.io.InputStream,android.content.Context);
 	java.io.InputStream data(android.content.Context);
}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.* extends player.phonograph.mechanism.tag.MetadataExtractor { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.* extends player.phonograph.mechanism.tag.TagReader { public *** read(...); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.edit.* extends player.phonograph.mechanism.tag.edit.EditAction { public *** valid(...); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.tag.edit.CommonKt { void applyEditImpl(...); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.* extends player.phonograph.mechanism.IFavorite { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.* extends player.phonograph.mechanism.IUriParser { public <methods>; }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.coil.retriever.FetcherDelegate { abstract <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking interface player.phonograph.coil.retriever.CacheStore$Cache { public <methods>; }