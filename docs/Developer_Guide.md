# **Developer Guide** / **开发指南**

This document describes the overview of this project for developers.

See also [Build Instruction](./Build_Instructions.md).

_Last Update: 2024.07.04_

## Toolchain & Dependencies

This is a pure kotlin Android Application.

Gradle Version Catalogs is used in this project:

Please refer [libs.versions.toml](../gradle/libs.versions.toml) for all the libraries and all gradle plugins.

**Toolchain**

- Gradlew `8.6`, requiring JDK `17`
- `Android Gradle Plugin` `8.4.2`
- Android SDK `34`
- kotlin for JVM(Android) `1.9.22`

**Libraries**

Highlight:

- `Jetpack Compose` 1.6.8
- `Jetpack Datastore` 1.0.0
- `kotlinx.serialization`
- `kotlinx.parcelize`
- `koin` as a lightweight Dependency Injection solution

See [List of Libraries and Gradle Plugins in Use](./List_of_Libraries.md) for details

## Build Variant

Currently, there are one Flavor Dimension `purpose` for different release channel, which has 3 variants[^1].

| Dimension `purpose` | Extra Package Name Suffix |                   Usage                   |       Note       |
|:-------------------:|:-------------------------:|:-----------------------------------------:|:----------------:|
|      `stable`       |         _(None)_          | **Stable** & **LTS**<br/> channel release |                  |
|      `preview`      |        `.preview`         |     **Preview**<br/> channel release      |                  |
|     `checkout`      |        `.checkout`        |         (`Github Action` Build )          | for locating bug |

[^1]: Before v0.4.0, there are more variants (like `common` as `stable`, `ci` for `Github Action`).

And, there are two default `BuildType`s (`debug`/`release`), and all `release` shrinks and minifies.

## Project Structure

#### Gradle Module

Currently:

- _app_(`app/`): all actual code of the Phonograph Plus
- _changelog-generator_(`tools/changelog-generator`): for generating changelog from

#### Repository Structure

Except gradle's file ():

- `.github/`: `Github Action` and templates
- `.idea/`: Android Studio's config including code style config and run config
- `app/`, `tools/changelog-generator`: Gradle Module
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

