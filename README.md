# mihon-eink

E-Ink optimized fork of [Mihon](https://github.com/mihonapp/mihon) for Android readers with black-and-white panels and low-refresh displays.

[![GitHub downloads](https://img.shields.io/github/downloads/paperjin/mihon-eink/total?label=downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/paperjin/mihon-eink/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/paperjin/mihon-eink/build.yml?labelColor=27303D)](https://github.com/paperjin/mihon-eink/actions)
[![License: Apache-2.0](https://img.shields.io/github/license/paperjin/mihon-eink?labelColor=27303D&color=0877d2)](/LICENSE)

## Download

[![mihon-eink Latest](https://img.shields.io/github/v/release/paperjin/mihon-eink.svg?maxAge=3600&label=Latest&labelColor=06599d&color=043b69)](https://github.com/paperjin/mihon-eink/releases/latest)

Requires Android 8.0 or higher.

## Confirmed Features

The following features are present in the current codebase and release builds:

- Status overlay in the reader with time, battery, Wi-Fi, and page count
- Monochrome theme as the default app theme
- Analytics and Crashlytics disabled by default
- E-Ink dithering and bitmap filtering always enabled
- Reader page flash for chapter changes
- Grayscale rendering toggle
- Reader volume-key debounce for page navigation
- Offline-first local reading, backup, and restore
- Standard Mihon features for library, browsing, tracking, downloads, and source management

The old settings/library pagination experiments are not part of this release.

## Build From Source

Prerequisites:

- Java 21
- Android SDK 37
- Android NDK, only if you need native deps
- Git

Quick build:

```bash
git clone https://github.com/paperjin/mihon-eink
cd mihon-eink

export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Project Notes

- Roadmap and longer-term ideas live in [TODO.md](./TODO.md).
- Release builds are published on GitHub Releases.

## License

Apache License 2.0. See [LICENSE](/LICENSE) for the full text.
