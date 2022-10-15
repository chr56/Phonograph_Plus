# Phonograph Plus

[![Crowdin](https://badges.crowdin.net/phonograph-plus/localized.svg)](https://crowdin.com/project/phonograph-plus)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/chr56/Phonograph_Plus/blob/release/LICENSE.txt)
[<img src="https://github.com/chr56/Phonograph/workflows/ci/badge.svg" alt="CI Status">](https://github.com/chr56/Phonograph_Plus/actions/workflows/ci.yml)

**Phonograph 第三方维护版**

**A fork of Phonograph under maintenance and development**

A material designed local music player for Android.

<br/>

This is a fork of [Phonograph](https://github.com/kabouzeid/Phonograph), with some extra additional features.

## **特性** / **Features**

建议直接看[更新日志](app/src/main/assets/changelog-ZH-CN.html)!

It is suggested to browser the [Changelog](app/src/main/assets/changelog.html) to learn all features completely.

- 解锁 Pro | Unlock pro.

- 自动夜间模式 | Automatic & adaptive dark mode.

- 调整界面 | Many changes to UI.

- 应用内手动更改语言 | Change language in application manually.

- 详情对话框内显示 Tag 信息 | Show tag information in "Detail" Dialog

- 歌词对话框内显示歌词时间轴信息, 并可以通过长按进行快速转跳与自动滚动 | Show Time Axis in "Lyrics" Dialog and allow seeking basing lyric's time axis and
  support lyrics following.

- 适配 Android 11 分区存储 （部分） | Fix Android 11 Scope Storage.(WIP)

- 适当折叠歌曲弹出菜单 | Optimise song item menu.

- 改进媒体库交互 | Improve “Library” pages user experience。

- 增大“最近播放”和“最喜爱的歌曲(实际是“最常播放”的歌曲)”条目数量(100→150) | Increase history played tracks and top played tracks entries capacity (
  100->150).

- 新增崩溃报告页面 | Handle app crash.

- 支持更多排序方式 | Support more sort orders.

- 在歌曲(或文件)弹出菜单中, 快速添加黑名单 | Add song menu shortcut to add new items to blacklist.

- 适配" [墨·状态栏歌词](https://github.com/Block-Network/StatusBarLyric) "Xposed 模块 | Co-work-with/Support StatusBar Lyric
  Xposed Module (api)
- 支持导出内部数据库以供备份 | Export internal databases for the need of backup.

- 允许标签固定并平铺 | Allow tabs fixed.

- 更新对话框样式 | Update dialogs style.

- 以及更多细小特性 | and more small features/fixes.

## **翻译**/**Translation**

Translate Phonograph Plus into your language -> [Crowdin](https://crowdin.com/project/phonograph-plus)

We have removed Swedish and Norwegian Nynorsk translations due to missing too many translations

## **截图**/**Screenshot**

仅供参考， 以实际为准

For reference only, actual app might be different

|                                       Card Player                                       |                                       Flat Player                                       |
| :-------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: |
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/05.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/08.jpg?raw=true) |

|                                         Drawer                                          |                                         Setting                                         |
| :-------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: |
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/01.jpg?raw=true) |

|                                          Songs                                          |                                         Folders                                         |
| :-------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: |
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/09.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/10.jpg?raw=true) |

|                                         Artists                                         |                                        Playlists                                        |
| :-------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: |
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/07.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/06.jpg?raw=true) |

|                                        Song Menu                                        |                                 Tag Editor (Deprecated)                                 |
| :-------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: |
| ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg?raw=true) | ![Screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots/04.jpg?raw=true) |

## **Build Instructions** / **构建指南**

_This part is not written very well._

Currently(2020.10.15), this project's toolchain&dependencies are:

- `Android SDK` `33` (no `NDK`), requiring `JDK` `11`
- `Gradlew` `7.5.1`
- `Android Gradle Plugin` `7.3.0`
- `kotlin` for JVM(ANdroid) `1.7.10`
- `kotlinx.serialization`,`kotlinx.parcelize`
- most popular `androidx`(`Jetpack`) components (most of them are latest)
- popular 3rd-party libraries available on (`MavenCentral` and `jitpack.io`), some might kind of old and unmaintained
- `unpopular` 3rd-party libraries: `AdrienPoupa`'s `jaudiotagger`, `coil`
- some modified libraries by me

and

- <del>`Jetpack Compose`</del> coming soon in next versions

### Requirement

**Build**:

1) a PC : any desktop operate system platform (only `Windows` and `Ubuntu 20.04` are tested), I am not sure if it works
   on `Android(Termux)`.
2) JDK 11 (we are targeting API 33)
3) connected network

**Development**:

plus `Android Studio` with correspond `Android Gradle Plugin`

### Instructions (Build with commandline)

`bash`(on Linux) and `powershell` (on Windows) are tested.

0) Download source code

1) install JDK

(JDK 17 is untested)

on Windows
```shell
winget install --id EclipseAdoptium.Temurin.11.JDK
# or other vendor's JDK
```

on Linix (Debian based)
```shell
apt-get install temurin-11-jdk
```

on Linux ( `Fedora` / `RedHat` / `SUSE` )
```shell
yum install temurin-11-jdk
```

2) change your shell to repository's root 
3) generate a new signing key or use your own

4) configure Signing Config 

create file `signing.properties` on repository's root:
```properties
storeFile=<your-signing-key-file-path->
storePassword=<keystore-password>
keyAlias=<key-alias>
keyPassword=<key-password>
```
replace <*> with yours

You can create `signing.properties` by command:
```shell
echo "storeFile=<your-signing-key-file-path->" >> ./signing.properties
echo "storePassword=<keystore-password>" >> ./signing.properties
echo "keyAlias=<key-alias>" >> ./signing.properties
echo "keyPassword=<key-password>" >> ./signing.properties
```

4) build

```shell
 ./gradlew assembleStableRelease --parallel
```

5) pick up file

built apk is in `./app/build/outputs/apk/stable/release/` with name `PhonographPlus_<VERSION>-stable-release.apk`

you can run 
```shell
./gradlew PublishStableRelease
```
to move apk to `./products/stableRelease` and rename to `Phonograph Plus_<VERSION>.apk`


## **开发计划**/**Development Plan (or Road Map?)**

<br/>

**Phonograph Plus** is (partially) migrating to 🚀 Jetpack Compose -> see Branch [Compose](https://github.com/chr56/Phonograph_Plus/tree/Compose)

**Phonograph Plus** 正在（部分）迁移至 🚀 Jetpack Compose -> 参见 [Compose](https://github.com/chr56/Phonograph_Plus/tree/Compose)

<br/>

## **TO-DO list**

**2022**

- [x] 重构文件视图 | Refactor File Fragment

- [x] 重构媒体库 UI | Refactor Library UI

- [x] 实现更好的播放频率计数 | Better 'My Top Songs' algorithm

- [x] 完成 Readme | Complete README

- [x] 重构后台音乐服务 | Refactor MusicService

- [x] 迁移 Glide 至 Coil | Migrate Glide to Coil

- [ ] 支持白名单机制 | Whitelist

- [ ] 将歌曲“详情” 迁移至 Compose ⭕WIP (基本完成) | Migrate Song Detail to Jetpack Compose (⭕WIP: Almost Done)

- [ ] 重构设置 UI | Refactor Setting UI

- [ ] 重构更新对话框 | Refactor Update Dialog

- [ ] 重构搜索 | Refactor Search

- [ ] 自定义歌曲点击行为 ⭕ | User-defined click behavior for songs ⭕

- [ ] 自建本地媒体数据库(使用 AndroidX Room) ⭕, 以解析多艺术家歌曲, 并解析 Tag 中 ‘;’, '&', '/', '\', ',' , 改进搜索 | Use AndroidX Room to build
  Media database, to parse multi-artists songs and ‘;’, '&', '/', '\', ',' in tags, and improve search result

- [ ] ...

**2023~2024(?)**

- [ ] 重写音乐标签编辑 | Rewrite Tag Editor

- [ ] 增强“播放列表详情”(支持搜索 ❌, 更好的修改本地列表方式 ❗WIP, 响应打开文件的 Intent ❌) | Enhance Playlist Detail: support search ❌, Better way to
  modify ❗WIP, handle intent of open (playlist) file ❌

- [ ] 检查文件 | Valid files

- [ ] 桌面歌词(?) | Desktop lyrics (?)

- [ ] 改进 SlidingMusicBar | improve SlidingMusicBar

- [ ] 尝试适配 FlyMe / EvolutionX(等一系类原生)状态栏歌词 | Support some Android's StatusBar lyrics, such as FlyMe / EvolutionX

- [ ] <del>完美适配 Android11+ 的文件访问(❌) | Adapter Android11+ File Permission perfectly</del>

- [ ] <del>部分重构(所谓的)"主题引擎" | Refactor so-called Theme Engine</del>

- [ ] <del>统计听歌频率 | Make songs listening statistics</del>

- [ ] ...

<br/>
<br/>
<br/>
<br/>
