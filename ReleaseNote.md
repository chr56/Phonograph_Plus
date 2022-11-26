**0.5.0 2022.11.27**

**CN**
- 修复 在大屏设备上，横屏时无法调整列表列数，且锁定在6列
- 修复 浅色主色调下搜索页面上文本和图标的对比度低 (#43)
- 修复 某些颜色异常
- 新增 播放列表快照: 追踪当前播放列表, 在列表被替换时记录并存储(重启应用后失效), 以供恢复, 仅存储最近的变化, 位于播放器菜单(播放列表历史)
- 新增 允许自定义位于列表中的歌曲或文件的点击行为: 共 8 种模式和 2 种额外, 可自定义点击列表项目时的操作 (#12, 部分#33, #44)
- 移除 选项“记住随机播放” (意义模糊，与自定义点击行为和某些特意播放冲突)
- 删除 选项“保护播放列表不被肆意更改” (添加了播放列表快照与自定义点击行为后已无用)
- 新增 从外部播放时显示确认对话框 (如, 从文件管理器打开): 选项类似于自定义点击行为
- 修复 从桌面捷径播放失效
- 优化 歌词搜索加载速度
- 翻译 更新俄语翻译(by ElinaAndreeva)
- 开发 重构播放队列管理器, 清理代码, 更新依赖, 使用VersionCatalog

**EN**
- Fix: on large tablets, the number of list columns cannot be adjusted and locked to 6 columns when the screen is landscaped
- Fix: low contrast color of the text and icon on Search Page if using a light primary color (close #43)
- Fix: some color glitch
- New: playing queue snapshot: track the current queue, record and save when playing queue is entirety replaced (keep till app rebooted), only store recent changes, to recovery queues, goto player menu (playing queue history)
- New: custom click-behavior: allow custom click-behavior for song items or file items in list: There are 8 modes and 2 extra behaviors allowing defining what player should do when clicking item in a list.  (close #12, #33 partially, #44)
- Remove: the option "Remember Shuffle" (It's a very vague option, causing conflict with some intended playing actions and custom click-behavior)
- Remove: the option "Keep the playing queue intact" (It's useless now since queue snapshot and custom click-behavior were added)
- New: a confirm dialog when playing from outside (like open from file explorer): The options are similar to custom click-behavior.
- Fix: playing from shortcut won't work
- Optimise: lyrics fetching and loading performance
- Translation: updated Russian (by ElinaAndreeva)
- Development: refactor Queue Manager, clean up code, update dependencies, using Gradle VersionCatalog