# **Build Instructions For Beginners** / **入门构建指南**

_This part is not written very well._

See also [Developer Guide](./Developer_Guide.md).

_Last Update: 2025.02.16_

## **Requirement**

**Build**:

1. a PC : any desktop operate system platform (only `Windows` and `Ubuntu 22.04` are tested), I am not sure if it works
   on `Android(Termux)` because jvm version.
2. JDK 17 (we are using AGP 8.7.2 with gradle 8.12.1).
3. The connected and fast network.

**Development**:

Plus `Android Studio` with correspond `Android Gradle Plugin` (currently `Ladybug | 2024.2.1 Patch 2`).
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

See Also Appendix [Generate A New Keystore for Signing](#generate_keystore)

### 5) configure Signing Config

create file `signing.properties` on repository's root:

(Replace <\*> with yours.)

```properties
storeFile=<your-signing-key-file-path->
storePassword=<keystore-password>
keyAlias=<key-alias>
keyPassword=<key-password>
```

See Also Appendix [Generate A New Keystore for Signing](#generate_signing_properties)

### 6) build

Now, we can build project in variant of `Modern` and `Stable` with Build Type `Release` now. [^f]

[^f]: See more in section _Build Variant_ from [Development Guild](./Developer_Guide.md#build-variants).

```shell
 ./gradlew assembleModernStableRelease --parallel
```

### 7) pick up file

Built apk is in `./app/build/outputs/apk/modernStable/release/` with name `PhonographPlus_<VERSION>-modern-stable-release.apk`

You can run

```shell
./gradlew PublishModernStableRelease
```

to move apk to `./products/ModernStableRelease` and rename to `Phonograph Plus_<VERSION>_ModernStableRelease.apk`

## Appendix

### Generate A New Keystore for Signing <a id="generate_keystore"></a>

Use `keytool` from JDK:

```shell
keytool -genkeypair -storepass <keystore-password> -alias <key-alias> -keypass <key-password> -keyalg RSA -keysize 2048 -keystore <your-signing-key-file-path->
```

### Generate `signing.properties` <a id="generate_signing_properties"></a>

You can create `signing.properties` by command:

(Replace <\*> with yours.)

```shell
echo "storeFile=<your-signing-key-file-path->" >> ./signing.properties
echo "storePassword=<keystore-password>" >> ./signing.properties
echo "keyAlias=<key-alias>" >> ./signing.properties
echo "keyPassword=<key-password>" >> ./signing.properties
```