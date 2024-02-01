# **Build Instructions For Beginners** / **入门构建指南**

_This part is not written very well._

See also [Developer Guide](./Developer_Guide.md).


## **Requirement**

**Build**:

1. a PC : any desktop operate system platform (only `Windows` and `Ubuntu 20.04` are tested), I am not sure if it works
   on `Android(Termux)` because jvm version.
2. JDK 17 (we are using AGP 8.2.2 with gradle 8.3).
3. The connected and fast network.

**Development**:

Plus `Android Studio` with correspond `Android Gradle Plugin` (currently `Hedgehog (Patch 2)`). 
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

### 7) pick up file

Built apk is in `./app/build/outputs/apk/stable/release/` with name `PhonographPlus_<VERSION>-stable-release.apk`

You can run

```shell
./gradlew PublishStableRelease
```

to move apk to `./products/stableRelease` and rename to `Phonograph Plus_<VERSION>.apk`

