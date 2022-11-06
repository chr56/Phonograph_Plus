**0.5-dev1 2022.11.6**

**CN**
- 修复 浅色主色调下搜索页面上文本和图标的对比度低 (#43)
- 新增 允许自定义位于列表中的歌曲或文件的点击行为: 共 8 种模式和 2 种额外, 可自定义点击列表项目时行为(#12, 部分#33, #44)
- 移除 选项“记住随机播放” (意义模糊，与自定义点击行为和某些特意播放冲突)
- 删除 选项“保护播放列表不被肆意更改” (添加了自定义点击行为后已无用)
- 新增 从外部播放时显示确认对话框 (如, 从文件管理器打开): 选项类似于自定义点击行为
- 修复 从桌面捷径播放失效
- 开发 清理代码

**EN**
- Fix: low contrast color of the text and icon on Search Page if using a light primary color (close #43)
- New: allow custom click-behavior for song items or file items in list: There are 8 modes and 2 extra behaviors allowing defining what player should do when clicking item in a list. (close #12, #33 partially, #44)
- Remove: the option "Remember Shuffle" (It's a very vague option, causing conflict with some intended playing actions and custom click-behavior)
- Remove: the option "Keep the playing queue intact" (It's useless now since custom click-behavior was added)
- New: a confirm dialog when playing from outside (like open from file explorer): The options are similar to custom click-behavior.
- Fix: playing from shortcut won't work
- Development: clean up code