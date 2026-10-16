# MeloX Linux（弥乐）

[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Arch%20Linux%20%7C%20Linux-1793D1?logo=linux)](#运行环境)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](#技术栈)

<p align="center">
  <img src="https://raw.githubusercontent.com/lladlam/MeloX-Android/main/android/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png" width="128" alt="MeloX icon" />
</p>

<p align="center">Kotlin + Compose Multiplatform 构建的 MeloX Linux 桌面客户端，面向 Arch Linux x86_64 和 AppImage。</p>

> [!IMPORTANT]
> **MeloX Linux 正在开发中。** 本项目优先复用 MeloX Windows 的 Kotlin Multiplatform 业务层和 Compose Desktop UI，并以 MeloX Android 的产品行为作为参考。Linux 构建已在 Arch Linux KDE Wayland 上启动验证；完整功能和不同发行版的兼容性仍在持续验证。

> MeloX Linux 是非官方开源项目，与网易云音乐、小米、Apple 及其关联公司不存在隶属、合作或授权关系。

## 项目说明

MeloX Linux 基于 [MeloX Android](https://github.com/lladlam/MeloX-Android) 的设计、交互与业务逻辑，以及 [MeloX Windows](https://github.com/lladlam/MeloX-Windows) 已移植的跨平台桌面代码。

项目目标是提供真正可用的 Linux 桌面音乐客户端，而非 WebView 外壳或静态界面：

- 使用 **Kotlin + Compose Multiplatform Desktop** 构建桌面 UI；
- 复用共享的账号、音乐源 Provider、搜索、歌词、资料库、播放解析、队列和设置逻辑；
- 使用 FFmpeg 解码网络或本地音频，并通过 JVM 音频设备输出；
- 将应用运行时和 Linux 音频解码器打包为 **x86_64 AppImage**；
- 以 MeloX Android 作为页面结构、播放器行为和视觉组件的主要对照。

## 当前实现

Linux 工程包含 Windows 桌面端的共享业务模块与 Compose UI 源码。当前实现范围包括：

- 多音乐源 Provider、登录状态与持久化；
- 搜索、首页/发现内容、歌曲和播放 URL 解析；
- 歌词数据模型与 LRC、YRC、TTML 等解析；
- 资料库、下载、设置和桌面导航；
- MiniPlayer、全屏播放器、队列及播放状态流；
- FFmpeg → PCM → Java Sound 的桌面音频输出路径；
- 以 `~/.melox` 保存用户数据，不依赖启动工作目录。

功能覆盖不代表每个 Provider 或服务接口始终可用。第三方服务、账号权限、地区及网络状况都可能影响结果。Android 与 Linux 的系统媒体会话、浮动歌词、窗口行为和 Glass 合成效果存在平台差异。

## 技术栈

| 用途 | 实现 |
| --- | --- |
| UI | Kotlin + Compose Multiplatform Desktop |
| 共享业务逻辑 | Kotlin Multiplatform (`shared`) |
| YouTube Music InnerTube | 独立 `innertube` 模块 |
| 网络 | OkHttp |
| 异步 | Kotlin Coroutines |
| 音频解码 | 随应用打包的 FFmpeg |
| 音频输出 | JVM Java Sound (`SourceDataLine`) |
| Linux 分发 | AppImage，x86_64 |

## 运行环境

- 目标：Arch Linux x86_64；其他 Linux 发行版尚未完成兼容性验收；
- JDK 17 用于开发和构建；
- Gradle Wrapper 会下载所需 Gradle 发行版；
- 图形桌面需提供 Compose/Skiko 所需的 X11 或 Wayland、OpenGL 和字体相关系统库；
- AppImage 携带 JVM 与 FFmpeg，但不包含所有图形驱动和桌面系统库。

## 从源码构建

```bash
git clone https://github.com/lladlam/MeloX-Linux.git
cd MeloX-Linux
git lfs install
git lfs pull
./gradlew :shared:build :desktop:compileKotlinDesktop
```

运行桌面应用：

```bash
./gradlew :desktop:run
```

构建 AppImage：

```bash
./packaging/build-appimage.sh
```

最终文件为 `MeloX-Linux-x86_64.AppImage`。在 Arch Linux 上启动：

```bash
chmod +x MeloX-Linux-x86_64.AppImage
./MeloX-Linux-x86_64.AppImage
```

## 项目结构

```text
.
├── desktop/
│   └── src/desktopMain/kotlin/melox/
│       ├── Main.kt                 # Linux 桌面入口
│       ├── ui/                     # 复用的 Compose Desktop UI
│       └── validation/             # UI / 状态验证工具
├── shared/
│   └── src/
│       ├── commonMain/kotlin/      # Provider、模型、播放、歌词、设置等
│       └── desktopMain/kotlin/     # JVM/Desktop 平台实现
├── innertube/                     # YouTube Music InnerTube 模块
├── packaging/                     # AppImage 启动器与构建脚本
└── gradle/libs.versions.toml      # Kotlin 与 Compose 版本
```

## 上游项目与致谢

- [lladlam/MeloX-Android](https://github.com/lladlam/MeloX-Android) — Android 产品、交互和业务逻辑参考；
- [lladlam/MeloX-Windows](https://github.com/lladlam/MeloX-Windows) — Kotlin Multiplatform 共享层与桌面 Compose 实现；
- [lladlam/MeloX](https://github.com/lladlam/MeloX) — MeloX 上游项目。

## 免责声明

本项目出于学习、研究与开源交流目的开发。使用者应遵守所在地法律法规、音乐服务条款及内容版权要求。第三方服务接口可能变化，开发者不保证持续可用。本项目按许可证所述不提供任何担保。

## 许可证

本项目按 GNU General Public License v3.0（GPL-3.0）发布，详见 [LICENSE](LICENSE)。
