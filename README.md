# SporeAdjust

Minecraft **1.21.1** 的 **NeoForge** 模组，用于调整 Spore 的战斗与增援机制，并与 Iron's Spellbooks 集成。

## 功能

- 感染者增援事件、波次与冷却管理。
- 诱惑物、强效诱惑物、镇静剂和信息素探测仪。
- 伤害限制、奉献和攻击欲望效果。
- 冰系与雷电法术对感染者的特殊交互。
- 孢子生物群系及 Overworld Mirror 维度生成配置。

## 开发环境

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.228
- Gradle Wrapper

项目使用本地第三方模组依赖。构建前请按 [libs/README.md](libs/README.md) 的版本清单，将依赖 JAR 放入 `libs/`。具体版本定义见 `gradle.properties`。

## 构建与运行

Windows：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Linux / macOS：

```sh
./gradlew build
./gradlew runClient
```

构建产物位于 `build/libs/`。第三方模组 JAR、运行存档、IDE 配置和构建缓存不纳入版本管理。

当前 GitHub Actions 构建工作流也需要取得这些本地依赖后才能成功运行。

## 作者

VittorioVeneto
