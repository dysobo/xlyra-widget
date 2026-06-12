# xLyra Widget

Android desktop widget for monitoring xLyra usage, request volume, token volume, model cost, and Codex quota.

## Features

- Native Android app written in Kotlin
- Compose settings and status screen
- Traditional `AppWidgetProvider + RemoteViews` desktop widget for better OEM launcher compatibility
- 4x3 home-screen widget layout
- WorkManager periodic refresh
- Manual refresh from the widget
- DataStore local cache
- EncryptedSharedPreferences token storage
- Supports self-hosted HTTP endpoints when configured by the user

## Widget Content

- Today cost
- Total cost
- Today requests
- Today tokens
- RPM / TPM
- Codex 5h remaining quota progress
- Codex 7d remaining quota progress
- Top two model costs
- Last refresh time

## Build

```powershell
.\gradlew.bat assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

```powershell
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/xlyra-widget.apk
adb shell pm install -r -g /data/local/tmp/xlyra-widget.apk
```

## Configuration

Open the app and configure:

- xLyra base URL
- Admin Access Token
- Refresh interval

The token is stored locally using encrypted preferences and is not committed to the repository.
