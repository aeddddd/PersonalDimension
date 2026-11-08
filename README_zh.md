# 个人维度（Personal Dimension）

一个独立的 Minecraft **1.20.1 / 1.21.1 NeoForge** 模组，为玩家提供属于自己的平坦维度：
可自定义地板预设与配色、多层布局、规则控制，以及异步重铺地板流水线。

## 功能

- **个人维度核心**：创建、进入与离开个人维度。
- **个人维度管理器**：集中管理规则、地板预设/配色与布局设置。
- **FTB Teams 联动**：默认同一队伍共用一个维度，每名成员可切换到自己独有的个人维度；
  未安装 FTB Teams 时回退为「所有者 / OP」权限模型。
- 不生成基岩、默认不刷怪、地板高度可配置、支持多层布局，
  重铺地板异步执行并按区块分摊预算。

## 依赖

- `1.20.1-neoforge`：Minecraft 1.20.1、NeoForge 47.1.106+、Java 17
- `1.21.1-neoforge`：Minecraft 1.21.1、NeoForge 21.1.265+、Java 21
- 可选：FTB Teams + FTB Library（队伍共享）、FTB Ranks（权限节点）

## 构建

```bash
./gradlew build
```

产物位于 `build/libs/`。

首次构建需要联网解析 ModDevGradle 与 NeoFormRuntime（部分网络环境无法访问
NeoForge 的 maven；已有完整 Gradle 缓存时可用 `--offline`）。

## 分支

- `1.20.1-neoforge` —— Minecraft 1.20.1 / NeoForge 47.x
- `1.21.1-neoforge` —— Minecraft 1.21.1 / NeoForge 21.1.x（当前）

## 许可

GNU General Public License v3.0。
