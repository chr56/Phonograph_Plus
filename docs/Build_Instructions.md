# **Build Instructions & Developer Guide** / **构建指南与开发指南**

_This part is not written very well._

Currently(2023.8.31), this project's toolchain & dependencies are:

- `Android SDK` `33` (no `NDK`)
- `Gradlew` `8.3`, requiring `JDK` `17`
- `Android Gradle Plugin` `8.1.1`
- `kotlin` for JVM(Android) `1.8.10`
- `kotlinx.serialization`,`kotlinx.parcelize`
- most popular `androidx`(`Jetpack`) components (most of them are latest)
- `Jetpack Compose` (Since 0.4)
- many 3rd-party libraries available on (`MavenCentral` and `jitpack.io`), some might kind of old and unmaintained
  including some modified libraries by me

Gradle Version Catalogs is used in this project.

see [libs.versions.toml](../gradle/libs.versions.toml) for all the libraries.

see [plugins.versions.toml](../gradle/plugins.versions.toml) for all gradle plugins.

## **Requirement**

**Build**:

1. a PC : any desktop operate system platform (only `Windows` and `Ubuntu 20.04` are tested), I am not sure if it works
   on `Android(Termux)` because jvm version.
2. JDK 17 (we are using AGP 8.1.1 with gradle 8.3).
3. The connected and fast network.

**Development**:

Plus `Android Studio` with correspond `Android Gradle Plugin` (currently `Giraffe (Patch 1)`). 
(`IDEA` might be not compatible because `Android Gradle Plugin` is too new)

## **Instructions (Build with commandline)**

`bash`(on Linux) and `powershell` (on Windows) are tested.

### 1) Download source code

a. go to release page to download parked release code

b. use `git`

```shell
git clone <REPO-URL> --depth=1 -b <VERSION>
```

### 2) install JDK

on Windows

```shell
winget install --id EclipseAdoptium.Temurin.17.JDK
# or JDK by other vendor
```

on Linux (`Debian` based)

```shell
apt-get install temurin-17-jdk
```

on Linux ( `Fedora` / `RedHat` / `SUSE` )

```shell
yum install temurin-17-jdk
```

### 3) change your shell to repository's root

### 4) generate a new signing key or use your own

using `keytool` from JDK

```shell
keytool -genkeypair -storepass <keystore-password> -alias <key-alias> -keypass <key-password> -keyalg RSA -keysize 2048 -keystore <your-signing-key-file-path->
```

### 5) configure Signing Config

create file `signing.properties` on repository's root:

```properties
storeFile=<your-signing-key-file-path->
storePassword=<keystore-password>
keyAlias=<key-alias>
keyPassword=<key-password>
```

replace <\*> with yours.

You can create `signing.properties` by command:

```shell
echo "storeFile=<your-signing-key-file-path->" >> ./signing.properties
echo "storePassword=<keystore-password>" >> ./signing.properties
echo "keyAlias=<key-alias>" >> ./signing.properties
echo "keyPassword=<key-password>" >> ./signing.properties
```

### 6) build

We are building the build variant `Stable` (Build Type `Release`) now.

See more in section Build Variant.

```shell
 ./gradlew assembleStableRelease --parallel
```

if your version is before 0.4, replace `stable` with `common` (matching letter case), using:

```shell
./gradlew assembleCommonRelease --parallel
```

### 7) pick up file

_Note: if the version is before 0.4, replace `stable` with `common` (matching letter case)_

built apk is in `./app/build/outputs/apk/stable/release/` with name `PhonographPlus_<VERSION>-stable-release.apk`

you can run

```shell
./gradlew PublishStableRelease
```

to move apk to `./products/stableRelease` and rename to `Phonograph Plus_<VERSION>.apk`

## Build Variant

only one flavor `purpose` and two default `BuildType` (`debug`/`release`), and all `release` shrinks and minifies.

|       Build Variant        |                                        Note                                        |
|:--------------------------:|:----------------------------------------------------------------------------------:|
|          `stable`          |                             for stable and LTS release                             |
| ~`common` (before v0.4.0)~ |                     for stable and LTS release (before v0.4.0)                     |
|         `preview`          |                for preview release, package name suffix `.preview`                 |
|         `checkout`         | for bug-locate and `dev` build of `Github Action`, package name suffix `.checkout` |

before v4.0, we have more (like `ci` for `Github Action`).

## Project Structure

#### Gradle Module

Currently:

- `app`: all actual code of the Phonograph Plus
- `release-tool`: (composite build) store libraries dependencies meta (versions etc) and some util for gradle build
  script

#### Source Code Structure of Phonograph Plus

TODO

#### Repository Structure

- `app/`, `tools/release-tool/`, `tools/changelog-generator`: Gradle Module
- `version.json`,`version_catalog.json`: containing the latest version information that Phonograph Plus would read at
  startup
- `crowdin.yml`: Crowdin configuration
- `ReleaseNote.md`: GitHub Action `preview_release` read this and post to Release Page
- `fastlane/metadata/android/`: F-droid metadata
- `.github/`: `Github Action` and templates
- and other gradle's file
