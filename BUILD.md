# SSTV Mochi — 换机编译说明

这个包是 `sstv/mochi-app/` 的源码，已剔除构建产物、`.gradle/`、`local.properties`。
`app/libs/robot36.jar` 和 `gradle/wrapper/` 都在，可以直接编。

## 1. 环境要求

| 项 | 版本 | 说明 |
|---|---|---|
| JDK | **17** | `compileOptions` / `jvmTarget` 都是 17，AGP 8.13.2 也要求 17+ |
| Android SDK | `platforms;android-36` | `compileSdk = 36`、`targetSdk = 36` |
| Build-Tools | `36.0.0` | |
| Gradle | 8.14.3 | 由 wrapper 自动下载，不用自己装 |

```bash
sdkmanager "platforms;android-36" "build-tools;36.0.0"
```

## 2. 必须先做的一步：写 local.properties

```bash
echo "sdk.dir=/你的/Android/Sdk" > local.properties
```

## 3. 第二件必须确认的事：aapt2 覆盖开关

原机器是 **aarch64**，SDK 里的 `aapt2` 是 x86_64 二进制，所以 `gradle.properties`
里有一行 `android.aapt2FromMavenOverride=/home/Gold/toolchain/aapt2`（qemu 包装脚本）。

**新机器是 x86_64 / Windows / macOS 时，这行必须是注释状态**（本包默认已注释）。
只有当你也在 aarch64 Linux 上编、并且遇到 `aapt2` 报 "Exec format error" 时，才需要
重新做一个同类包装脚本并把这一行指过去。

## 4. 编译

```bash
chmod +x gradlew
./gradlew assembleDebug      # 输出 app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # 需要签名配置，见下
```

首次编译要联网下 Gradle 发行版和依赖（compose-bom 2025.04.01、mochi-compose 0.2.0 等）。

## 5. 签名（release 才需要）

`app/build.gradle.kts` 里 `signingConfigs.release` 写死了：

```
storeFile     = rootProject.file("sstv-mochi.jks")
storePassword = sstvmochi
keyAlias      = sstv
keyPassword   = sstvmochi
```

keystore 证书（PKCS12，别名 `sstv`，CN=SSTV Mochi）有效期到 **2054-04-02**，
SHA256 `8B:9F:56:66:30:69:8C:22:00:35:66:36:E3:5C:A7:E2:58:F4:64:8F:7A:B2:13:B6:C9:7B:A5:E7:5D:9A:E5:55`。

**`sstv-mochi.jks` 不在这个包里**（公网链接不放私钥），已单独打成加密包；
下载后用下面的命令解开，放到 `mochi-app/sstv-mochi.jks` 再 `assembleRelease`：

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -in sstv-mochi-keys.tar.enc -out sstv-mochi-keys.tar
tar xf sstv-mochi-keys.tar        # 得到 sstv-mochi.jks
```

只想出个能装的包、不在意覆盖升级的话，直接用 `assembleDebug` 即可，不需要 keystore。

## 6. 已知差异

- `versionName` 是 `1.1.1-robot36`，`versionCode` 是 `6`。
- 仓库里带 `.github/workflows/build.yml`，推到 GitHub 也能直接出 artifact
  （该 workflow 走 `assembleRelease`，因此仓库里需要有 jks）。
