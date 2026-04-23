# **Version Guide**: Which Version to Download?

**_RL;DR_**: If you are a user of Android 7~10, use `Legacy`; If not, use `Modern`. Besides, `Fdroid` is exactly identical with apk on Fdroid;
ignore it.

Currently, we have three channel (`Stable` Channel, `Preview` Channel, and `Fdroid` Channel that is identical on F-droid),
and two distinct Variants:

- `Modern`
- `Legacy`

We make this distinction mostly for bypassing _Scope Storage_ for Android 10.

### `Modern` (for mainstream android device user)

It targets on the latest SDK, and declares `hasFragileUserData` true (supporting uninstalling without removing data).

### `Legacy` (for legacy android device user)

Target on SDK 28 (Android 9), to bypass _Scope Storage_ introduced in Android 10
(especially helpful for Android 10 users since _Scope Storage_ could not be _escaped_ in this version).
Also, it declares `hasFragileUserData` false, to solve the problem of difficulty uninstalling[^ud],
due to installer(uninstaller) crashing on Android 10 with physical SD card.

[^ud]: See related [FAQ](./FAQ.md#problems-with-uninstalling)

<br/>
<br/>

See also [Development Guild $ Build Variants](./Developer_Guide.md#build-variants)

--- 

# About Devices with Play Protect

### Android Developer Verification

> Starting in 2027 worldwide (or September 2026 for some countries),
> Android applications from developers without verification
> centrally through Google Play or the Android Developer Console,
> _may no longer be installed easily_ on certified Android devices.

In simple terms, APKs signed by unregistered signing keys, where developers refuse to upload their personal identification (governmental ID etc.)
and their app information (package names and signing key fingerprints etc.) to Google, _may not install easily_ on devices with Google Mobile
Services (GMS) where Play Protect is enforced.

### Impact on us

Apps distributed outside Google Play, just like Phonograph Plus, are severely affected. Although an “advanced flow” may exist, **the "
sideloading" would be much more difficult for most users**. And Google is determined without any sign of regret for now.

Details and updates can
be found [here (offical)](https://developer.android.com/developer-verification) and [here (3rd-party)](https://keepandroidopen.org/).

### Future plan

As a response to this policy, after **April 1st 2027** (or August 10th 2027), APK artifacts **will no longer be provided in releases but only
source code**.
Please compile and build Phonograph Plus _yourself_.

If Android becomes further closed and restricted, **Phonograph Plus may be really and eventually discontinued in 2028**, depending on Google's
policies next.

