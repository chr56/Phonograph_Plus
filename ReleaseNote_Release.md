## {{version:0.7.0-dev2}} {{versionCode:535}} {{date:1685806700}} {{channel:preview}}

### {{note:en}}
- New: manually load lyrics
- New: add more options in Main Drawer Menu
- New: support per-app language preference for Android T (13) and above
- New: support sorting order of playlist (but basic sorting)
- New: support multi-selection in Search Result
- New: support pinning playlists
- Fix: new created playlist would not be appeared in list util next entrance
- Fix: palette color of player is incorrect or lacking of update
- Fix: app language is trapped in or out the spelling of British English (for English users only) 
- Fix: sort order of flat-folders could not be remembered
- Translation: update Japanese (by aorinngo)
- Development: refactor Setting UI using Jetpack Compose
- Improve: redesign Setting UI and make minor changes
- Improve: avoid random crash caused by the file browser
- Improve: Title and Buttons of some dialog (like Upgrade Dialog) are not fixed but scrollable with dialog content
- Development: use Jetpack Datastore Preference
- Development: refactor player ui, lyrics loading and more
- Development: improve dispatching the changes of states
- Development: upgrade JDK to 17
- Development: upgrade Gradle to 8.1, AGP to 8.0.2, enable `nonTransitiveRClass`

### {{note:zh}}
- 新增 手动加载歌词
- 新增 在抽屉主菜单中增加更多选项
- 新增 支持 Android T (13) 的系统级分应用语言设置
- 新增 支持播放列表的排序（基本排序支持）
- 新增 搜索结果支持多选
- 新增 支持播放列表置顶
- 修复 新建的播放列表不会立即出现在列表中
- 修复 播放器色调着色不正确或未更新
- 修复 语言卡在英式拼写里面或外面 (针对英语用户)
- 修复 无法记住扁平文件夹的排序方式
- 翻译 更新日语 (by aorinngo)
- 开发 设置界面使用 Jetpack Compose 重构
- 改进 设置界面并进行微调
- 改进 防止由于文件浏览器导致的崩溃
- 改进 部分对话框（如升级对话框）的标题和按钮无法固定，而随内容一起滚动
- 开发 使用 Jetpack Datastore Preference
- 开发 重构播放器界面，歌词加载等
- 开发 改进状态分发
- 开发 升级JDK到17
- 开发 更新 Gradle 至 8.1, 更新 AGP 至 8.0.2, 启用 `nonTransitiveRClass`