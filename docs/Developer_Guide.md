# **Developer Guide** / **开发指南**

This document describes the overview of this project for developers.

See also [Build Instruction](./Build_Instructions.md).

_Last Update: 2026.01.31_

## Toolchain & Dependencies

This is a Kotlin Android Application.

Gradle Version Catalogs is used in this project:

Please refer [libs.versions.toml](../gradle/libs.versions.toml) for all the libraries and all gradle plugins.

**Toolchain**

- Gradlew `8.14.3`, along with JDK `21`
- `Android Gradle Plugin` `8.13.2`
- Android SDK `36`
- kotlin for JVM(Android) `2.2.20`

**Libraries**

Highlight:

- `Jetpack Compose`
- `Jetpack Datastore`
- `kotlinx.serialization`
- `kotlinx.parcelize`
- `koin` as a lightweight Dependency Injection solution

See [List of Libraries and Gradle Plugins in Use](./List_of_Libraries.md) for details

## Build Variants

Since 1.8[^1], we have two Flavor Dimension currently: `channel` (for different release channels), `target` (for Android platforms).

And, there are two default `BuildType`s (`debug`/`release`), and all `release` shrinks and minifies.

[^1]: Before 1.8, there is only one flavor `purpose`/`channel`.

#### Dimension `channel`

| Dimension `channel`[^2] | Extra Package Name Suffix |                     Usage                     | Note              |
|:-----------------------:|:-------------------------:|:---------------------------------------------:|-------------------|
|        `stable`         |         _(None)_          |   **Stable** & **LTS**<br/> channel release   |                   |
|        `fdroid`         |         _(None)_          |    **Fdroid** _reproducible build_ release    | based on `stable` |
|        `preview`        |        `.preview`         |       **Preview**<br/> channel release        |                   |
|         `next`          |          `.eap`           | **Early Access Preview**<br/> channel release |                   |
|       `checkout`        |        `.checkout`        |           (`Github Action` Build )            | for locating bug  |

[^2]: Before v0.4.0, there are more variants (like `common` as `stable`, `ci` for `Github Action`).

#### Dimension `target`

We make this distinction mostly for bypassing _Scope Storage_ for Android 10.

| Dimension `target` | Target SDK | Min SDK | Descriptions                                                                           |
|:------------------:|:----------:|:-------:|----------------------------------------------------------------------------------------|
|      `modern`      | _(Latest)_ |   24    | for mainstream android device users                                                    |
|      `legacy`      |     28     |   24    | for legacy android device user <br/>(especially Android 10, to bypass _Scope Storage_) |

## Project Structure

#### Gradle Module

Currently:

- _app_(`app/`): source code of the Phonograph Plus
- _changelog-generator_(`tools/changelog-generator`): utility for generating formated changelogs and release notes

#### Repository Structure

Except files relative to gradle or building:

- `.github/`: `Github Action` and templates
- `.idea/`: Android Studio's config including code style config and run config
- `app/`, `tools/changelog-generator`: Gradle Module
- `docs/`: documents
- `scripts/`: utility scripts
- `fastlane/metadata/android/`: F-droid metadata, like summary, screenshot, changelogs
- `crowdin.yml`: Crowdin configuration
- `ReleaseNote.yaml`, `ReleaseNoteStable.yaml`: Pending release notes as well as metadata, used for generating changelogs and release note everywhere on releasing 
- `fdroid.properties`: metadata of current latest version, unused
- `version_catalog.json`: containing the latest version metadata that Phonograph Plus used for checking updates

#### Source Code Structure of Phonograph Plus

TODO

