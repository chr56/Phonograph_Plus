#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class lib.phonograph.preference.** {*;}

-keep class player.phonograph.preferences.** {*;}
-keep class player.phonograph.views.** {*;}
-keep class player.phonograph.model.** {*;}

-keep,allowoptimization,allowshrinking class player.phonograph.ui.** {public <methods>;public <fields>;}
-keep,allowoptimization,allowshrinking class player.phonograph.service.** {public <methods>;public <fields>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.adapter.** {public <methods>;<fields>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.dialogs.** {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.util.** {public <methods>;public <fields>;<init>(...);}
-keepnames,allowoptimization,allowshrinking class player.phonograph.glide.** {<init>(...);public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.** {public <methods>;}
-keepclassmembernames,allowoptimization class player.phonograph.notification.** {public <methods>;}
