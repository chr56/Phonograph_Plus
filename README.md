# Phonograph Plus

[![Crowdin](https://badges.crowdin.net/phonograph-plus/localized.svg)](https://crowdin.com/project/phonograph-plus)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/chr56/Phonograph_Plus/blob/release/LICENSE.txt)
[<img src="https://github.com/chr56/Phonograph_Plus/actions/workflows/dev.yml/badge.svg" alt="Dev CI Status">](https://github.com/chr56/Phonograph_Plus/actions/workflows/dev.yml)

**Phonograph 第三方维护版**

**A fork of Phonograph under maintenance and development**

A material designed local music player for Android.

<br/>

This is a fork of [Phonograph](https://github.com/kabouzeid/Phonograph), and is currently under my maintenance and development.

## **新增特性** / **Additional Features**

所有特性皆与原版相比。 All listed features are compared with original Phonograph.

- 解锁 Pro | Unlock pro.

- 自动夜间模式 | Automatic & adaptive dark mode.

- 大规模调整界面 | Plenty of user interface changes.

- 应用内手动更改语言 | Change language in application manually.

- 全新详情页, 显示歌曲标签等信息 | Brand-new Detail page with more information like tags

- 歌词对话框内显示歌词时间轴信息, 并可以通过长按进行快速转跳与自动滚动 | Show Time Axis in "Lyrics" Dialog and allow seeking basing lyric's time axis and
  support lyrics following.

- 自定义歌曲点击行为 | User-defined click behavior for songs

- 支持正在播放列表历史记录 | Support history of playing queue.

- 适配 Android 11 分区存储 （部分） | Fix Android 11 Scope Storage. (Partial)

- 改进菜单, 适当折叠歌曲弹出菜单 | Improve menu experience.

- 改进媒体库交互 | Improve “Library” pages user experience.

- 支持更多排序方式 | Support more sort orders.

- 添加崩溃报告页面 | Allow collecting app crash report.

- 使用路径过滤器代替黑名单, 支持排除模式与仅包含模式 | use Path filter to replace, support "exclude mode" (blacklist) and "include mode" (whitelist).

- 在歌曲(或文件)弹出菜单中, 快速添加排除名单(黑名单) | Add song menu shortcut to add new items to excluded-list (blacklist).

- 适配" [墨·状态栏歌词](https://github.com/Block-Network/StatusBarLyric) "Xposed 模块 | Co-work-with/Support StatusBar Lyric
  Xposed Module (api)

- 支持导出内部数据库以供备份 | Export internal databases for the need of backup.

- 增大“最近播放”和“最喜爱的歌曲(实际是“最常播放”的歌曲)”条目数量(100→150) | Increase history played tracks and top played tracks entries capacity (
  100->150).

- 允许标签固定并平铺 | Allow tabs fixed.

- 全新的文件夹视图 | Brand-new File tab.

- 支持删除歌曲时一同删除歌词 | Allow deleting songs file along with its external lyrics file. 

- 以及更多细小特性 | and more small features/fixes.


浏览[更新日志](https://phonographplus.github.io/changelogs/changeslogs/changelog-ZH-CN.html)以了解明细!

It is suggested to browser the [Changelog](https://phonographplus.github.io/changelogs/changeslogs/changelog.html) to learn all features completely

## **翻译**/**Translation**

Translate Phonograph Plus into your language -> [Crowdin](https://crowdin.com/project/phonograph-plus)

We have removed Swedish and Norwegian Nynorsk translations due to missing too many translations

## **截图**/**Screenshot**

仅供参考， 以实际为准

For reference only, actual app might be different

|                                       Card Player                                       |                                       Flat Player                                       |
|:---------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/05.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/08.jpg?raw=true) |

|                                         Drawer                                          |                                         Setting                                         |
|:---------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/01.jpg?raw=true) |

|                                          Songs                                          |                                         Folders                                         |
|:---------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/09.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/10.jpg?raw=true) |

|                                         Artists                                         |                                        Playlists                                        |
|:---------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/07.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/06.jpg?raw=true) |

|                                        Song Menu                                        |                                 Tag Editor (Deprecated)                                 |
|:---------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/04.jpg?raw=true) |

## **构建指南与开发指南** / **Build Instructions & Developer Guide**

See [Build_Instructions.md](./Build_Instructions.md)

## **开发计划**/**Development Plan (or Road Map?)** & **TO-DO list**

### **2022**

- [x] 重构文件视图 | Refactor File Fragment

- [x] 重构媒体库 UI | Refactor Library UI

- [x] 实现更好的播放频率计数 | Better 'My Top Songs' algorithm

- [x] 完成 Readme | Complete README

- [x] 重构后台音乐服务 | Refactor MusicService

- [x] 迁移 Glide 至 Coil | Migrate Glide to Coil

- [x] 支持白名单机制 | Whitelist

- [x] 将歌曲“详情” 迁移至 Compose | Migrate Song Detail to Jetpack Compose

- [x] 重构更新对话框 | Refactor Update Dialog

- [x] 自定义歌曲点击行为 | User-defined click behavior for songs

- [x] 支持正在播放列表历史记录 | Support history of playing queue.

- [ ] 重构设置 UI (WIP⭕) | Refactor Setting UI (WIP⭕)

- [ ] 重构搜索 | Refactor Search

- [ ] 自建本地媒体数据库(使用 AndroidX Room) ⭕, 以解析多艺术家歌曲, 并解析 Tag 中 ‘;’, '&', '/', '\', ',' , 改进搜索 | Use AndroidX Room to build
  Media database, to parse multi-artists songs and ‘;’, '&', '/', '\', ',' in tags, and improve search result

- [ ] ...

### **2023~2024(?)**

- [ ] 重写音乐标签编辑 (单文件⭕, 批量❌) | Rewrite Tag Editor (Single File⭕, Batch❌)

- [ ] 重构主播放器 (WIP⭕) | Refactor Main Player (WIP⭕)

- [ ] 增强“播放列表详情”(支持搜索 ❌, 更好的修改本地列表方式 ❗WIP, 响应打开文件的 Intent ❌) | Enhance Playlist Detail: support search ❌, Better way to
  modify ❗WIP, handle intent of open (playlist) file ❌

- [ ] 桌面歌词(?) | Desktop lyrics (?)

- [ ] 尝试适配 FlyMe / EvolutionX(等一系类原生)状态栏歌词 | Support some Android's StatusBar lyrics, such as FlyMe / EvolutionX

- [ ] 改进 SlidingMusicBar | improve SlidingMusicBar

- [ ] <del>检查文件 | Valid files</del>

- [ ] <del>完美适配 Android11+ 的文件访问(❌) | Adapter Android11+ File Permission perfectly</del>

- [ ] <del>部分重构(所谓的)"主题引擎" | Refactor so-called Theme Engine</del>

- [ ] <del>统计听歌频率 | Make songs listening statistics</del>

- [ ] ...

<br/>
<br/>
<br/>
<br/>
