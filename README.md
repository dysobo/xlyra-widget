# xLyra 桌面卡片

一个用于 Android 桌面的 xLyra 用量监控小组件。主 App 负责配置和刷新数据，桌面卡片用于快速查看用量、成本、流量和 Codex 剩余额度。

## 功能

- 原生 Android Kotlin 项目
- 主界面使用 Jetpack Compose
- 桌面组件使用传统 `AppWidgetProvider + RemoteViews`
- 兼容荣耀等厂商桌面，避免 Glance 在部分系统上显示异常
- 多个桌面卡片布局：4x3、4x2、4x1、2x1
- WorkManager 后台定时刷新
- 桌面卡片支持手动刷新
- DataStore 保存配置和本地缓存
- EncryptedSharedPreferences 加密保存 Admin Token
- 支持用户自行配置自托管 xLyra 地址

## 桌面卡片

可选小组件：

- `xLyra 用量 4x3`：完整信息，包含成本、请求、Tokens、RPM/TPM、5h/7d 进度条、Top2 模型。
- `xLyra 用量 4x2`：美化紧凑版，显示今日成本 + Tokens、累计成本、5h/7d 百分比、Top2 模型。
- `xLyra 成本 4x1`：只显示今日成本 + Tokens、累计成本。
- `xLyra 今日 2x1`：只显示今日成本 + Tokens。
- `xLyra 配额 2x1`：只显示 5h / 7d 剩余额度进度条。

## 构建

```powershell
.\gradlew.bat assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装

普通安装：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

如果厂商系统拦截 `adb install`，可以使用：

```powershell
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/xlyra-widget.apk
adb shell pm install -r -g /data/local/tmp/xlyra-widget.apk
```

## 使用

1. 安装并打开 App。
2. 填写 xLyra 服务地址。
3. 填写 Admin Access Token。
4. 设置刷新间隔。
5. 点击保存并立即刷新。
6. 回到桌面添加需要的 `xLyra` 小组件。
7. 如果需要完整信息，推荐使用 4x3；如果追求美观紧凑，推荐使用 4x2。

## 安全说明

项目不会在源码中保存任何真实 Token。

- 服务地址、刷新间隔、缓存数据保存在 DataStore。
- Admin Token 使用加密偏好存储。
- 本地凭据文件、构建产物、截图和调试文件已通过 `.gitignore` 排除。

## 接口

当前使用 xLyra 的轻量摘要接口：

```http
GET /api/v1/dashboard/epaper-summary
X-Access-Token: <Admin Access Token>
```

桌面组件不会直接请求网络。数据刷新流程为：

```text
App / Worker 请求接口
  -> 写入本地缓存
  -> 更新 RemoteViews 桌面组件
```
