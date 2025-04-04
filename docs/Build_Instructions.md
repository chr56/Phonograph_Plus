# **Build Instructions for Beginners** / **入门构建指南**

## Overview

_This document seeks to improve clarity and coherence._

See also [Developer Guide](./Developer_Guide.md).

_Last Updated at 2025.04.04_

## Requirements

### Build Environment:

You may would like to build the application from source instead of developing and contributing. 

- A desktop computer [^PC]:
    - at least 4G RAM
    - enough available storage space, on user home disk (at least ~2G)  
- Compatible desktop operating system [^PC]:
    - Windows 10/11 (tested)
    - Ubuntu 22.04 (tested)
- JDK 17 (AGP 8.8.2 with gradle 8.12.1 are used in this project)
- Android SDK (automatically downloadable in entire process)
- Internet connection 

[^PC]: Compatibility is uncertain on Termux; Android 16 with full Linux VM may be compatible.

### Development Environment:

If you would like to contribute:

- Android Studio Ladybug Feature Drop | 2024.2.2 Patch 2
  (Note: JetBrain IDEA may not be compatible due to its limited support on latest Android Gradle Plugin) 

## Build Instructions

(`bash` or `powershell`)

### 1. Obtain Source Code

Choose one of the following methods:

- Download from release page (`Source code (zip/tar.gz)`)
- Clone using Git:
  ```shell
  git clone <REPO-URL> --depth=1 -b <VERSION>
  ```

### 2. Install JDK

Temurin by Eclipse Adoptium Foundation for example:

##### Windows

```shell
winget install --id EclipseAdoptium.Temurin.17.JDK
```

##### Linux (Debian / Ubuntu / ...)

```shell
apt-get install temurin-17-jdk
```

##### Linux (Fedora / RedHat / SUSE)

```shell
yum install temurin-17-jdk
```

### 3. Change Directory to Repository Root

```shell
cd <SOURCE>
# Set-Location <SOURCE>
```

### 4. Generate or Prepare Signing Key

See [Generate A New Keystore for Signing](#generate_keystore) if you hadn't.

### 5. Configure Signing Configuration

Create `signing.properties` in the repository root (replace <\*> with yours):

```properties
storeFile=<path-to-signing-key>
storePassword=<keystore-password>
keyAlias=<key-alias>
keyPassword=<key-password>
```

See Also Appendix [Generate `signing.properties`](#generate_signing_properties)

### 6. Build Project

Now, we can build project in variant of `Modern` and `Stable` with Build Type `Release` now. [^f]

[^f]: See more in section _Build Variant_ from [Development Guild](./Developer_Guide.md#build-variants).

```shell
./gradlew assembleModernStableRelease
```

### 7. Locate Output Artifacts

Built apk is in `./app/build/outputs/apk/modernStable/release/` with name `PhonographPlus_<VERSION>-modern-stable-release.apk`

You can run

```shell
./gradlew PublishModernStableRelease
```

to move apk to `./products/ModernStableRelease` and rename to `Phonograph Plus_<VERSION>_ModernStableRelease.apk`

## Appendices

### Generate A New Keystore for Signing <a id="generate_keystore"></a>

Use JDK's `keytool` to generate a new one if you haven't:

```shell
keytool -genkeypair -storepass <keystore-password> -alias <key-alias> -keypass <key-password> -keyalg RSA -keysize 2048 -keystore <your-signing-key-file-path->
```

### Generate `signing.properties` <a id="generate_signing_properties"></a>

You can create `signing.properties` by these commands (replace <\*> with yours):

```shell
echo "storeFile=<your-signing-key-file-path>" >> ./signing.properties
echo "storePassword=<keystore-password>" >> ./signing.properties
echo "keyAlias=<key-alias>" >> ./signing.properties
echo "keyPassword=<key-password>" >> ./signing.properties
```