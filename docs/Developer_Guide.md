# **Developer Guide** / **开发指南**

This document describes the overview of this project for developers.

See also [Build Instruction](./Build_Instructions.md).

_Last Update: 2023.09.16_

## Toolchain & Dependencies

This is a pure kotlin Android Application.

Gradle Version Catalogs is used in this project:

see [libs.versions.toml](../gradle/libs.versions.toml) for all the libraries.

see [plugins.versions.toml](../gradle/plugins.versions.toml) for all gradle plugins.

**Platform**

-   Gradlew `8.3`, requiring JDK `17`
-   `Android Gradle Plugin` `8.1.1`
-   Android SDK `34`
-   kotlin for JVM(Android) `1.9.10`

**Libraries**

-   `Jetpack Compose` 1.5.1 (Since 0.4, only few ui are written in Compose)
-   `kotlinx.serialization`,`kotlinx.parcelize`
-   `koin` as a lightweight Dependency Injection solution
-   most popular `androidx`(`Jetpack`) components (most of them are latest)
-   many 3rd-party libraries available on (`MavenCentral` and `jitpack.io`), some might kind of old and unmaintained
    including some modified libraries by me

## Build Variant

only one flavor `purpose` and two default `BuildType` (`debug`/`release`), and all `release` shrinks and minifies.

|       Build Variant        |                                        Note                                        |
| :------------------------: | :--------------------------------------------------------------------------------: |
|          `stable`          |                             for stable and LTS release                             |
| ~`common` (before v0.4.0)~ |                     for stable and LTS release (before v0.4.0)                     |
|         `preview`          |                for preview release, package name suffix `.preview`                 |
|         `checkout`         | for bug-locate and `dev` build of `Github Action`, package name suffix `.checkout` |

before v4.0, we have more (like `ci` for `Github Action`).

## Project Structure

#### Gradle Module

Currently:

-   _app_(`app/`): all actual code of the Phonograph Plus
-   _changelog-generator_(`tools/changelog-generator`): for generating changelog from
-   _release-tool_(`tools/release-tool/`): (composite build) store libraries dependencies meta (versions etc) and some util for gradle build
    script

#### Repository Structure

Except gradle's file ():

- `.github/`: `Github Action` and templates
- `.idea/`: Android Studio's config including code style config and run config
- `app/`, `tools/release-tool/`, `tools/changelog-generator`: Gradle Module
- `docs/`: documents
- `scripts/`: bash scripts for ci
- `fastlane/metadata/android/`: F-droid metadata, like summary, screenshot, changelogs
- `version_catalog.json`: containing the latest version information that Phonograph Plus would read at
- startup
- `crowdin.yml`: Crowdin configuration
- `LICENSE.txt`: GPL licenses
- `ReleaseNote.toml`: GitHub Action `preview_release` read this and post to Release Page


#### Source Code Structure of Phonograph Plus

TODO

