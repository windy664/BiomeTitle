# BiomeTitle

Velocity 代理端插件，玩家跨群系边界时显示 Title 提示。

## 功能

- 玩家移动时检测群系变化，自动发送 Title
- 从服务端 REGISTRY_DATA 包获取真实群系名称，不依赖客户端语言文件
- 支持群系黑名单（默认过滤海洋和河流）
- 标题颜色、分隔线、显示时间均可配置

## 依赖

- Velocity 3.3.0+
- Java 21
- [packetevents](https://modrinth.com/plugin/packetevents)（打包进 jar，与其他插件共享）

## 构建

```bash
./gradlew shadowJar
```

产物在 `build/libs/BiomeTitle-1.0.0.jar`，丢到 Velocity 的 plugins 目录即可。

## 配置

首次启动会在 `plugins/BiomeTitle/` 下生成 `config.yml`：

```yaml
fade-in: 15        # 淡入时间 (tick)
stay: 50           # 停留时间
fade-out: 20       # 淡出时间
title-color: FFAA00
divider-color: 888888
show-divider: true
divider-text: "───── ✦ ─────"
blacklist:
  - biome.minecraft.ocean
  - biome.minecraft.river
  # ...
```

## 群系名称

插件拦截服务端发来的 `REGISTRY_DATA` 配置包，直接读取群系的翻译 key。1.21.4+ 服务端会把完整注册表发给客户端，所以代理端也能拿到。
