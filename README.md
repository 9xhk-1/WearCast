# WearCast

腕上投屏哔哩哔哩视频 —— 面向小天才等 320×360 方屏 Android 手表的 B 站客户端。

## 功能

- **扫码登录**：手表上生成 B 站登录二维码，手机扫码后 Cookie 持久化保存
- **搜索 / 推荐 / 收藏**：统一卡片列表，显示封面、标题与 UP 主
- **视频详情**：简介、播放/弹幕/评论数据与热门评论
- **DLNA 投屏**：自动发现同一 Wi-Fi 下的电视/音箱，投屏后支持
  - 播放 / 暂停、快进 / 快退 10 秒
  - 清晰度切换、音量调节
  - 记住上次设备（按当前局域网区分），下次自动重连
- **个人中心**：展示头像、昵称与等级，支持退出登录

## 安装

### 方式一：下载 Release（推荐）

在 [Releases](https://github.com/9xhk-1/WearCast/releases) 下载最新的 `app-release.apk`，
通过手表的文件管理器安装，或使用 ADB：

```bash
adb install -r app-release.apk
```

### 方式二：无线 ADB 安装

手表打开「开发者选项 → 无线调试」，确保手表和电脑在同一 Wi-Fi：

```bash
adb connect <手表IP>:5555
adb install -r app-release.apk
adb shell am start -n com.wearcast.app/.ui.SplashActivity
```

> 部分小天才手表关闭了截图接口，可用 `uiautomator dump` 检查界面层级。

## 构建

环境要求：

- JDK 17+
- Android SDK Platform 34、Build-Tools 34.0.0
- Gradle 8.7（仓库自带 Wrapper）

```bash
# 本地调试包（自动使用 debug 签名）
./gradlew assembleDebug

# Release 包：未配置签名时产出 unsigned APK
./gradlew assembleRelease
```

配置 Release 签名（可选，推荐用于持续升级安装）：

| 环境变量 | 说明 |
| --- | --- |
| `SIGN_STORE_FILE` | keystore 文件路径 |
| `SIGN_STORE_PASSWORD` | keystore 密码 |
| `SIGN_KEY_ALIAS` | 密钥别名 |
| `SIGN_KEY_PASSWORD` | 密钥密码 |

签名配置后，后续版本可直接覆盖安装；否则每次 CI 会生成一次性签名，升级前需要先卸载旧版。

## 持续集成 / 发布

`.github/workflows/build-apk.yml` 会在以下时机构建：

- 推送到 `main` / 任意 Pull Request：构建 APK 并上传为 Actions artifact
- 推送 `v*` 标签：构建并自动创建 GitHub Release，附带 APK

发布新版本：

```bash
# 1. 修改 app/build.gradle 中的 versionCode / versionName
# 2. 提交后打标签
git tag v1.1.0
git push origin v1.1.0
```

仓库 Secrets 中已配置 `SIGNING_KEY_BASE64`、`SIGNING_STORE_PASSWORD`、
`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`，CI 使用固定密钥签名。

## 技术栈

- Java + Android Gradle Plugin 8.5
- Retrofit + OkHttp + Gson 调用哔哩哔哩 Web API
  - 自动补充 `buvid3/buvid4` 设备 Cookie
  - 自动计算 Wbi 签名（`wts` + `w_rid`）
  - 按接口设置正确的 `Referer`
- 自实现 SSDP 发现 + UPnP AVTransport / RenderingControl SOAP 控制
- ZXing 生成登录二维码，Glide 加载封面 / 头像

## 目录结构

```
app/src/main/java/com/wearcast/app/
├── cast/       SSDP 发现、DLNA 控制、设备记忆
├── model/      B 站接口数据模型
├── network/    Retrofit 服务、Cookie、Wbi 签名、仓库层
└── ui/         搜索 / 推荐 / 收藏 / 个人中心 / 详情 / 投屏界面
```

## 已知限制

- 仅支持 DLNA/UPnP `MediaRenderer` 设备；Chromecast 等私有点播协议不支持
- 视频以 MP4（`durl`）形式投屏，暂不支持 DASH 清晰度切换
- 收藏页默认展示第一个收藏夹
