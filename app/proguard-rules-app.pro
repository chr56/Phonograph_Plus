#
#  Phonograph
#

-keep class player.phonograph.ui.views.** {*;}
-keep class player.phonograph.model.** {public <fields>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.adapter.DisplayPresenter { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.adapter.DisplayAdapter { protected <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.basis.** extends android.app.Activity { protected <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.LauncherActivity { void goto*(); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.MigrationActivity { void *Impl();}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.PhonographIntroActivity$* { private <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.auxiliary.StarterActivity { private <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.explorer.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.main.pages.AbsPanelPage { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.main.pages.AbsDisplayPage { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.main.pages.AbsDisplayPage$AbsDisplayPageViewModel { public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.panel.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.player.** { protected <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.modules.** extends androidx.lifecycle.ViewModel {public <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking interface player.phonograph.repo.loader.IFavoriteSongs { public <methods>; }
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

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.lyrics.LyricsLoader { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.migrate.Migration { public void doMigrate(android.content.Context); }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.metadata.* extends player.phonograph.mechanism.metadata.MetadataExtractor { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.metadata.* extends player.phonograph.mechanism.metadata.TagReader { public *** read(...); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.metadata.edit.* extends player.phonograph.model.metadata.EditAction$Executor { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.* extends player.phonograph.mechanism.IUriParser { public <methods>; }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.coil.retriever.FetcherDelegate { abstract <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking interface player.phonograph.coil.cache.CacheStore$Cache { public <methods>; }