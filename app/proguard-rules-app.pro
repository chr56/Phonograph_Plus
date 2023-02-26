#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class lib.phonograph.preference.*PreferenceX {*;}

-keepclasseswithmembernames class lib.phonograph.activity.** {public <methods>; public <fields>;}


-keep class player.phonograph.preferences.*PreferenceX {*;}
-keep class player.phonograph.views.** {*;}

-keep class player.phonograph.model.** {public <fields>;}

-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.activities.** extends android.app.Activity {public <methods>;}

-keepclasseswithmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.pages.AbsDisplayPage {<methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.pages.PlaylistPage {<methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.pages.**Page extends player.phonograph.ui.fragments.pages.AbsDisplayPage
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.player.** extends androidx.fragment.app.Fragment {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.SettingsFragment {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.HomeFragment {public <methods>;}

-keepclassmembernames class player.phonograph.ui.components.explorer.*Explorer {public <methods>;}
-keepclassmembernames class player.phonograph.ui.components.popup.ListOptionsPopup {public <methods>;}

-keepclassmembernames class player.phonograph.ui.dialogs.DeleteSongsDialog {public <methods>;}
-keepclassmembernames class player.phonograph.ui.dialogs.PathFilterDialog {public <methods>;}

-keepclasseswithmembernames class player.phonograph.ui.** extends androidx.lifecycle.ViewModel {public <methods>;}


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mediastore.QueryUtilKt {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mediastore.ModelTransformKt { <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.MusicService {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.notification.** {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.AudioPlayer {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.player.PlayerController {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.queue.QueueManager {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.util.** {public <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.AlbumCoverPagerAdapter {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.HomePagerAdapter {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.sortable.PageTabConfigAdapter {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.PlaylistAdapter {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.display.** {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.base.** {public <methods>;public <fields>;}

-keepnames,allowoptimization,allowshrinking class player.phonograph.dialogs.**

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.** {public <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.Setting {public void set**(...); public *** get**();}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.migrate.SettingDataManager {public <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.notification.**Impl {public <methods>;}

-keepclassmembernames,allowoptimization class player.phonograph.provider.DatabaseManger { boolean *(...);}


-keepclasseswithmembernames,allowoptimization,allowshrinking class util.phonograph.tageditor.** {public <methods>;public <fields>;}
