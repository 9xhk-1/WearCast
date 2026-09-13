# WearCast
腕上投屏bilibili视频

一个面向 360x320 圆形/方形手表屏幕的 Android 应用，用于搜索、浏览哔哩哔哩视频并投屏到同一局域网内的电视/DLNA 设备。

## 功能

- 扫码登录哔哩哔哩账号，Cookie 持久化保存
- 搜索、推荐、收藏视频列表，统一卡片风格，展示封面缩略图、标题与作者
- 视频详情页展示简介、统计信息与评论
- 点击播放后自动投屏到 DLNA/UPnP 电视设备
  - 首次使用需在同局域网内选择设备
  - 之后在同一 Wi-Fi 网络下自动记住并重连上次选择的设备，也可随时切换或断开
- 投屏控制页支持播放/暂停、快进快退、清晰度切换与音量调节
- 个人中心展示登录用户信息

## 技术栈

- Java + Gradle（Android Gradle Plugin）
- Retrofit + OkHttp + Gson 访问哔哩哔哩 Web API（接口参考 xtcqinghe/bac 整理的文档）
- 自实现的 SSDP 发现与 UPnP AVTransport/RenderingControl SOAP 控制，用于投屏
- ZXing 生成登录二维码，Glide 加载封面/头像

## 构建

项目已附带 Gradle Wrapper，使用 `./gradlew assembleRelease` 即可构建。CI 通过 GitHub Actions（`.github/workflows/build-apk.yml`）自动打包 APK：

- 若在仓库 Secrets 中配置了 `SIGNING_KEY_BASE64`（Base64 编码的 keystore 文件）、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`，则使用该签名密钥。
- 若未配置，则自动生成一个随机的一次性签名密钥用于打包。

