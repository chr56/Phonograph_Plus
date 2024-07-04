# **版本指南**: 该下载哪个版本？

**太长不看版**: 如果您使用的是 Android 7~10，请使用 `Legacy` 版本；否则，请使用 `Modern` 版本。

目前现有两个通道（`稳定` 通道和 `预览` 通道），以及两种不同的变体：

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