#
#  Phonograph
#

-keep class player.phonograph.ui.views.** {*;}

-keepnames,allowoptimization,allowshrinking class player.phonograph.model.* {*;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.model.backup.** {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.model.file.** {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.model.lyrics.** {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.model.metadata.** {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.model.playlist.** {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.model.repo.loader.* {public <methods>;}
-keepnames,allowoptimization,allowshrinking interface player.phonograph.model.service.** { <methods>; }

-keepnames,allowoptimization,allowshrinking class player.phonograph.foundation.mediastore.*

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
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.NavigationUtil { public <methods>; }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.MusicService { public <methods>;public <fields>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.notification.** { public <methods>;public <fields>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.VanillaAudioPlayer { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.PlayerController { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.PlayerController$VanillaAudioPlayerControllerImpl { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking interface player.phonograph.service.player.* { <methods>; }


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.lyrics.LyricsLoader { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.metadata.* extends player.phonograph.mechanism.metadata.MetadataExtractor { public <methods>; }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.metadata.* extends player.phonograph.mechanism.metadata.TagReader { public *** read(...); }
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mechanism.* extends player.phonograph.mechanism.IUriParser { public <methods>; }

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.coil.retriever.FetcherDelegate { abstract <methods>; }