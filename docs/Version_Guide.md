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