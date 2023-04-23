## {{version:0.6.2.1}} {{versionCode:521}} {{date:1682260000}} {{channel:stable}}

### {{note:en}}
- Note: Feature "Show Album Cover on Lockscreen" would be removed in next versions! (Applications should not be responsible for this!)
- Fix: Progress bar on Notification would not be refreshed after rewinding to beginning
- Modify: On Android R (11) and above, always ignore preference "Show Album Cover on Lockscreen", to fix incorrect aspect ratio of Notification Image (due to conflict)
- Improve: Image quality on Media Notification [for Android T]
- Development: On Android R (11) and above, set Notification Image via setting MediaSession Metadata Album Artwork
- Development: refactor Media Notification Image loader mechanises

### {{note:zh}}
- 注意 "锁屏上显示专辑封面"功能即将移除(此功能理应由系统负责)
- 修复 倒退到起始处后，通知进度条不刷新
- 修改 对于Android R (11)及以上，一律忽略设置 "锁屏上显示专辑封面"，并修复媒体通知的图片的长宽比异常（功能冲突）
- 改进 媒体通知的图像质量[仅限Android T]
- 开发 对于Android R (11)及以上，使用 MediaSession Metadata 来设置通知图片
- 开发 重构媒体通知的图片加载机制