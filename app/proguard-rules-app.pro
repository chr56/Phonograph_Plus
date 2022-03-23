#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class lib.phonograph.preference.** {*;}
-keepclasseswithmembernames class lib.phonograph.activity.** {*;}

-keep,allowoptimization,allowshrinking class util.phonograph.tageditor.** {public <methods>;public <fields>;}


-keep class player.phonograph.preferences.** {*;}
-keep class player.phonograph.views.** {*;}
-keep class player.phonograph.model.** {*;}

-keep,allowoptimization,allowshrinking class player.phonograph.ui.activities.** {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.ui.fragments.** {public <methods>;public <fields>;}


-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.service.** {public <methods>;public <fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.adapter.** {public <methods>;<fields>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.dialogs.** {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.util.** {public <methods>;public <fields>;<init>(...);}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.glide.** {<init>(...);public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.** {public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.notification.** {public <methods>;}

-keepclassmembernames,allowoptimization class player.phonograph.provider.DatabaseManger