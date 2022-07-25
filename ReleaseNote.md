**Preview 2022.7.25**

**CN**
与前一预览版相比:
- 修复: 因点击歌曲过快而导致的崩溃
- 开发: 重构 MusicService
- 开发: 正在播放歌曲界面歌曲封面使用 ViewPager2
- 新增: 添加菜单项, 以支持在 正在播放界面 中修改当前 正在播放界面 样式

**注意**：
从此版本起, MusicService 被重构, 也意味着存在大量潜在的与音乐播放相关错误, 如播放控件失效,当前正在播放的歌曲与ui不对应,通知异常,虚空播放等!
如有发现请尽快报告!

**EN**
Compared to the previous preview:
- Fix: Crash caused by clicking songs too fast
- Development: Refactor MusicService
- Development: Use ViewPager2 for song cover in the playing song interface
- New: Added a menu item to change the current Now Playing Screen in the Now Playing Screen

**Note**:
MusicService has been refactored since this version, this means there could be a HUGE number of potential bugs related to music playing, such as fails of playback controls, incorresponding current playing song with ui, notification glitches, ghostly playing... and crashes.
If they occurs to you, please report this bugs as soon as you can!