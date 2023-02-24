# Phonograph Plus

[![Crowdin](https://badges.crowdin.net/phonograph-plus/localized.svg)](https://crowdin.com/project/phonograph-plus)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/chr56/Phonograph_Plus/blob/release/LICENSE.txt)
[<img src="https://github.com/chr56/Phonograph_Plus/actions/workflows/dev.yml/badge.svg" alt="Dev CI Status">](https://github.com/chr56/Phonograph_Plus/actions/workflows/dev.yml)

**Phonograph 第三方维护版**

**正在维护和开发的 Phonograph 分支**

<br/>

[ENGLISH](./README.md)

原项目：[Phonograph](https://github.com/kabouzeid/Phonograph)

## **新增特性**

所有特性皆与原版相比。 

- 解锁 Pro 

- 自动夜间模式 

- 大规模调整界面 

- 应用内手动更改语言 

- 全新详情页, 显示歌曲标签等信息 

- 歌词对话框内显示歌词时间轴信息, 并可以通过长按进行快速转跳与自动滚动 

- 自定义歌曲点击行为 

- 支持正在播放列表历史记录 

- 适配 Android 11 分区存储 （部分） 

- 改进菜单, 适当折叠歌曲弹出菜单  

- 改进媒体库交互 

- 支持更多排序方式 

- 添加崩溃报告页面 

- 使用路径过滤器代替黑名单, 支持排除模式与仅包含模式 

- 在歌曲(或文件)弹出菜单中, 快速添加排除名单(黑名单)

- 适配" [墨·状态栏歌词](https://github.com/Block-Network/StatusBarLyric) 

- 支持导出内部数据库以供备份 
- 增大“最近播放”和“最喜爱的歌曲(实际是“最常播放”的歌曲)”条目数量(100→150)

- 允许标签固定并平铺 

- 全新的文件夹视图 

- 支持删除歌曲时一同删除歌词 

- 以及更多细小特性 

浏览[更新日志](https://phonographplus.github.io/changelogs/changeslogs/changelog-ZH-CN.html)以了解明细!


## **翻译**

[Crowdin](https://crowdin.com/project/phonograph-plus)


## **截图**

仅供参考， 以实际为准


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

## **构建指南与开发指南** 

[Build_Instructions.md](./Build_Instructions.md)

## **开发计划**

### **2022**

- [x] 重构文件视图

- [x] 重构媒体库 UI

- [x] 实现更好的播放频率计数

- [x] 完成 Readme

- [x] 重构后台音乐服务

- [x] 迁移 Glide 至 Coil

- [x] 支持白名单机制

- [x] 将歌曲“详情” 迁移至 Compose

- [x] 重构更新对话框

- [x] 自定义歌曲点击行为

- [x] 支持正在播放列表历史记录 

- [ ] 重构设置 UI (WIP⭕) 

- [ ] 重构搜索 

- [ ] 自建本地媒体数据库(使用 AndroidX Room) ⭕, 以解析多艺术家歌曲, 并解析 Tag 中 ‘;’, '&', '/', '\', ',' , 改进搜索

- [ ] ...

### **2023~2024(?)**

- [ ] 重写音乐标签编辑 (单文件⭕, 批量❌) 

- [ ] 重构主播放器 (WIP⭕) 

- [ ] 增强“播放列表详情”(支持搜索 ❌, 更好的修改本地列表方式 ❗WIP, 响应打开文件的 Intent ❌)

- [ ] 桌面歌词(?) 

- [ ] 尝试适配 FlyMe / EvolutionX(等一系类原生)状态栏歌词 

- [ ] 改进 SlidingMusicBar 

- [ ] <del>检查文件 </del>

- [ ] <del>完美适配 Android11+ 的文件访问(❌) </del>

- [ ] <del>部分重构(所谓的)"主题引擎" </del>

- [ ] <del>统计听歌频率 </del>

- [ ] ...

<br/>
<br/>
<br/>
<br/>
