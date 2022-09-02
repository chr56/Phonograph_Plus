#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class lib.phonograph.preference.*PreferenceX {*;}
-keepclasseswithmembernames class lib.phonograph.activity.** {public *;}

-keepclasseswithmembernames,allowoptimization,allowshrinking class util.phonograph.tageditor.** {public <methods>;public <fields>;}


-keep class player.phonograph.preferences.*PreferenceX {*;}
-keep class player.phonograph.views.** {*;}
-keep class player.phonograph.model.** {public <fields>;}

-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.activities.** extends android.app.Activity {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.** extends androidx.fragment.app.Fragment {public <methods>;}
-keepclasseswithmembernames class player.phonograph.ui.** extends androidx.lifecycle.ViewModel {public <methods>;}


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mediastore.MediaStoreUtil {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.mediastore.ModelTransformKt { <methods>;}

-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.** {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.** {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.dialogs.** {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.** {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.Setting {public void set**(...); public *** get**();}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.SettingManager {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.notification.** {public void post(...);}

-keepclassmembernames,allowoptimization class player.phonograph.Updater$VersionJson {public <methods>;}
-keepclassmembernames,allowoptimization class player.phonograph.provider.DatabaseManger { boolean *(...);}