# **版本指南**: 该下载哪个版本？

**太长不看版**: 如果您使用的是 Android 7~10，请使用 `Legacy` 版本；否则，请使用 `Modern` 版本。另外，带 `Fdroid` 为 F-droid 上完全相同的版本，请忽略。

目前现有两个通道（`稳定` 通道, `预览` 通道, 以及与 F-droid 上相同的 `Fdroid` 通道），以及两种不同的变体：

- `Modern`
- `Legacy`

作区分是为了绕过 Android 10引入 中的 _Scope Storage_。

### `Modern`（适用于主流安卓设备用户）

Target SDK 为最新，并将 `hasFragileUserData` 设置为 true（支持卸载时不删除数据）。

### `Legacy`（适用于旧版安卓设备用户）

Target SDK 为 28（Android 9），以绕过 Android 10 中引入的 _Scope Storage_（尤其是 绕过 Android 10 不完善的 _Scope Storage_）。
此外，将 `hasFragileUserData` 设置为 false，以解决在存在物理 SD 卡 Android 10 上的卸载问题[^ud]。

[^ud]: 参见相关 [FAQ](./FAQ.md#problems-with-uninstalling)

<br/>
<br/>

另请参阅 [开发指南](./Developer_Guide.md#build-variants)


# Play Protect 与安装限制

### Android Developer Verification

_中国大陆机型可能并不受影响（~或更惨~）_

> 从2027年起全球范围内（某些国家为2026年9月），未通过 Google Play 或 Android 开发者控制台
> 提交开发者身份验证信息的 Android 应用**无法“容易”安装**在认证的 Android 设备上。

简单来说，若开发者未将个人身份证明证件和包名与签名密钥指纹等应用信息提交至 Google，其未认证签名的 APK 文件可能**无法“容易”安装**在搭载 Google Mobile Service (GMS) 并启用了 Play Protect 的设备上。


### 影响

通过 Google Play 以外渠道分发的应用（如 Phonograph Plus）将受到严重影响；尽管未来可能存在“高级流程”，但 **对大多数用户而言，“侧载”将变得更加困难**；目前，Google 意向坚定不动摇。

更多细节与跟进请关注 [官方页面](https://developer.android.com/developer-verification) 和 [第三方页面](https://keepandroidopen.org/)。

### 未来计划

回应此政策，计划从 **2027年4月1日**（或 **2027年8月10日**）起，**不再发布 APK 安装包，仅发布源代码**；请自行编译Phonograph Plus。

如果 Android 进一步封闭和限制，取决于 Google 的后续政策，Phonograph Plus 可能最终在**2028**年起**彻底停更**。