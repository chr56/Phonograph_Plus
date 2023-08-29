# Phonograph Plus

[![Crowdin](https://badges.crowdin.net/phonograph-plus/localized.svg)](https://crowdin.com/project/phonograph-plus)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/chr56/Phonograph_Plus/blob/release/LICENSE.txt)
[<img src="https://github.com/chr56/Phonograph_Plus/actions/workflows/dev.yml/badge.svg" alt="Dev CI Status">](https://github.com/chr56/Phonograph_Plus/actions/workflows/dev.yml)
![DownloadsStatistics](https://img.shields.io/github/downloads/chr56/Phonograph_Plus/total)

- [ENGLISH](./README.md)
- [简体中文](./README_ZH.md)

<br/>

**Phonograph 第三方维护版**

正在维护和开发的 [Phonograph](https://github.com/kabouzeid/Phonograph) 独立分支



## **下载**

[<img src="https://img.shields.io/github/v/release/chr56/phonograph_plus?label=Github%20Release" alt="Github%20Release">](https://github.com/chr56/Phonograph_Plus/releases/latest)
[<img src="https://img.shields.io/github/v/release/chr56/phonograph_plus?label=Github%20Release%20(Latest)&include_prereleases" alt="Github%20Release%20(Latest)">](https://github.com/chr56/Phonograph_Plus/releases/)
[<img src="https://img.shields.io/github/v/release/chr56/phonograph_plus?label=F-droid" alt="F-droid">](https://f-droid.org/packages/player.phonograph.plus/)


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

- 支持导出内部数据库和设置以供备份

- 增大“最近播放”和“最喜爱的歌曲(实际是“最常播放”的歌曲)”条目数量(100→150)

- 允许标签固定并平铺

- 全新的文件夹视图

- 支持删除歌曲时一同删除歌词

- 全新的音乐标签编辑器

- 全新介绍页

- 改善Android T 以上的原生系统的通知栏图片质量

- 改进播放列表支持

- ~简陋的~ Monet 支持

- 倍数播放支持

- 打断后自动恢复播放

- 增强多选

- Android Auto 支持(仅基本功能)

- 以及更多细小特性

浏览[更新日志](https://phonographplus.github.io/changelogs/changeslogs/changelog-ZH-CN.html)以了解明细!

## **翻译**

[Crowdin](https://crowdin.com/project/phonograph-plus)

## **截图**

仅供参考， 以实际为准

|                                           Card Player                                           |                                           Flat Player                                           |                                           Drawer                                            |
|:-----------------------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/CardPlayer.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/FlatPlayer.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/Drawer.jpg?raw=true) |

|                                           Drawer                                            |                                             Songs                                             |                                             Files                                             |
|:-------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/Drawer.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/SongPage.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/FilePage.jpg?raw=true) |

|                                             Artists                                             |                                             Albums                                             |                                             Playlists                                             |
|:-----------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/ArtistPage.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/AlbumPage.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/PlaylistPage.jpg?raw=true) |

|                                           Setting                                            |                                           Song Detail                                           |                                           Tag Editor                                           |
|:--------------------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------:|
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/Setting.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/SongDetail.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/TagEditor.jpg?raw=true) |

## **构建指南与开发指南**

[Build_Instructions.md](docs/Build_Instructions.md)

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

- [x] 重构设置 UI

- [x] 重构搜索

- [ ] 自建本地媒体数据库(使用 AndroidX Room) ⭕, 以解析多艺术家歌曲, 并解析 Tag 中 ‘;’, '&', '/', '\', ',' , 改进搜索

### **2023~2024(?)**

- [x] 重写音乐标签编辑

- [x] 改进数据备份

- [x] 重构各页面

- [ ] 改进介绍页 (WIP🚧)

- [ ] 重构主播放器

- [ ] 增强“播放列表详情”

- [ ] 改进 SlidingMusicBar

- [ ] 尝试适配 FlyMe / EvolutionX(等一系类原生)状态栏歌词

- [ ] <del>检查文件 </del>

- [ ] <del>完美适配 Android11+ 的文件访问 </del>

- [ ] <del>部分重构(所谓的)"主题引擎" </del>

- [ ] <del>统计听歌频率 </del>

- [ ] ...

## **仓库镜像**

[![GitHub](https://img.shields.io/badge/Git-Github-Blue)](https://github.com/chr56/Phonograph_Plus/)
[![Codeberg](https://img.shields.io/badge/Git-Codeberg-Blue)](https://codeberg.org/PhonographPlus/Phonograph_Plus)
[![BitBucket](https://img.shields.io/badge/Git-BitBucket-Blue)](https://bitbucket.org/phonograph-plus/phonograph_plus/)
