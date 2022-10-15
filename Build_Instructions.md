# **Build Instructions & Developer Guide** / **构建指南与开发指南**

_This part is not written very well._

Currently(2020.10.15), this project's toolchain&dependencies are:

- `Android SDK` `33` (no `NDK`), requiring `JDK` `11`
- `Gradlew` `7.5.1`
- `Android Gradle Plugin` `7.3.0`
- `kotlin` for JVM(Android) `1.7.10`
- `kotlinx.serialization`,`kotlinx.parcelize`
- most popular `androidx`(`Jetpack`) components (most of them are latest)
- popular 3rd-party libraries available on (`MavenCentral` and `jitpack.io`), some might kind of old and unmaintained
- `unpopular` 3rd-party libraries: `AdrienPoupa`'s `jaudiotagger`, `coil`
- some modified libraries by me

and

- <del>`Jetpack Compose`</del> coming soon in next versions


see [Libs.kt](./version-management/src/java/version/management/Libs.kt) for all the libraries.

see [settings.gradle.kts](./settings.gradle.kts) for all gradle plugins.

## **Requirement**

**Build**:

1) a PC : any desktop operate system platform (only `Windows` and `Ubuntu 20.04` are tested), I am not sure if it works
   on `Android(Termux)`.
2) JDK 11 (we are targeting API 33)
3) connected network

**Development**:

plus `Android Studio` with correspond `Android Gradle Plugin`

## **Instructions (Build with commandline)**

`bash`(on Linux) and `powershell` (on Windows) are tested.

### 1) Download source code

a. go to release page to download parked release code 

b. use `git`

```shell
git clone <REPO-URL> --depth=1 -b <VERSION> 
```

### 2) install JDK

(JDK 17 is untested)

on Windows

```shell
winget install --id EclipseAdoptium.Temurin.11.JDK
# or JDK by other vendor
```

on Linux (Debian based)

```shell
apt-get install temurin-11-jdk
```

on Linux ( `Fedora` / `RedHat` / `SUSE` )

```shell
yum install temurin-11-jdk
```

### 3) change your shell to repository's root

### 4) generate a new signing key or use your own

### 5) configure Signing Config

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

### 6) build

We are building the build variant`Stable` now. see more in section Build Variant.

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

only one flavor `purpose` and two default `BuildType` (`debug`/`release`), and
all `release` shrinks and minifies.

`stable` (or `common` before v4.0): for stable and LTS release

`preview`: for preview release, package name suffix `.preview`

`checkout`: for locate-bug-propose and `dev` build of `Github Action`, package name suffix `.checkout`

before v4.0, we have more (like `ci` for `Github Action`).


