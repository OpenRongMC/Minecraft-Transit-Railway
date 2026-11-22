## Minecraft Transit Railway 4.0.5

_Minecraft Transit Railway_ is a [Minecraft mod](https://minecraft.wiki/w/Mods) that allows you to build your own transport network with automated trains, boats, cable cars, and planes!

This workspace targets the Fabric port for Minecraft 1.21.11. The vehicle simulation code is separated from the Minecraft instance, reducing lag on Minecraft servers.

## Major Features

- Custom train configuration
- Realistic bogies and train movement
- Improved signalling
- Sideways elevators
- OBJ features ported over from Nemo's Transit Expansion
- Vehicle swaying effect
- New online system map and OneBusAway integration
- Client-side FPS improvements and server-side TPS improvements

[See these features in detail.](https://www.youtube.com/playlist?list=PLp0jZgheSlXuQCXu9UynrKuBnq2Ef_TSI)

[![Video Trailer](https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/video-preview.png)](https://www.youtube.com/watch?v=1cZfU7t4cAk)

Please report any issues or bugs you find; that would be greatly appreciated! Refer to the [todo list](https://github.com/jonafanho/Minecraft-Transit-Railway/projects/2) to see currently known issues.

## Unofficial 1.21.11 Fabric builds (Neo-Minecraft-Transit-Railway)

This fork publishes Fabric jars for **Minecraft 1.21.11 + MTR 4.0.5**. See GitHub Releases.

### patch2 (`v4.0.5-1.21.11-patch2`)

- **Rails vanishing / flickering**: disable incorrect occlusion + camera-frustum culling that hid rails; capture live view/projection matrices for optimized rail shaders (beyond the patch1 FOV fix).
- **Lift link lines**: draw button/panel ↔ track links as segmented world-space lines while holding a linker (including Yunzhu-compatible linker class names).
- **Item models**: selected-state connectors/removers use `mtr:legacy_predicate` `range_dispatch` for 1.21.11 item model loading.
- **Diagnostics**: optional F8 toggle (`key.mtr.render_diagnostics`) for render diagnostics logging.

### patch1 (`v4.0.5-1.21.11-patch1`)

- Dynamic FOV projection for rails/doors/signs/elevators; ticket barrier glass flaps; escalator chain refresh.

## Downloads and Installation

Head over to the [Modrinth page](https://modrinth.com/mod/minecraft-transit-railway) to download the mod or to see the project information.

## Guide

There is a [new wiki](https://wiki.minecrafttransitrailway.com/start) for the mod. Take a look.

## Recommended Addons

- [MTR London Underground Addon](https://modrinth.com/mod/the-tube)
- [Joban Client Mod](https://modrinth.com/mod/jcm)
- [Station Decoration](https://modrinth.com/mod/station-decoration)
- [Russian Metro Addon](https://modrinth.com/mod/russian-metro-addon)

## Contributing

### Help Translate the Mod!

The [Crowdin site for the Minecraft Transit Railway mod](https://crwd.in/minecraft-transit-railway) is available!

Crowdin is a cloud-based platform for translators to contribute to a project. With your help, we can translate the mod into many different languages. You may create a free account to start translating.

[![Crowdin](https://badges.crowdin.net/minecraft-transit-railway/localized.svg)](https://crowdin.com/project/minecraft-transit-railway)

### Adding Features

1. Fork this project
2. On your fork, create a new branch based on the development version branch
3. Commit your changes to the new branch
4. Make a Pull Request to merge your branch into the development version of this repository

### Building

This checkout targets **Minecraft 1.21.11 / Fabric / MTR 4.0.5 only**. Use Java 21 or newer, Node.js with npm, the local dependency jars in `libs/`, and the sibling `../Minecraft-Mappings/` checkout with its 1.21.11 generated mixins. This is a multi-project porting workspace, not a standalone upstream build.

Run these commands in the project root, in order. Compile the website before embedding it into the mod:

```sh
./gradlew :fabric:setupWebsiteFiles
npm ci --prefix website
npm run build --prefix website
./gradlew :fabric:setupFiles
./gradlew :fabric:build
```

After website-only changes, run the npm build and `./gradlew :fabric:setupWebserverResources :fabric:build`. Embedding is local and fails if `index.html` is missing, rather than generating an empty website.

See `docs/WORKSPACE.md` for the directory layout, addon build commands, audit scope, and maintenance results.

The mod jar file should be generated in the following directory:

```
<root>/build/release/MTR-fabric-4.0.5+1.21.11.jar
```

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT). All [Noto fonts](http://www.google.com/get/noto/), bundled with this mod, are licensed under the [Open Font License](http://scripts.sil.org/OFL).

## Questions? Comments? Complaints?

Let's connect.

<a href="https://discord.gg/PVZ2nfUaTW" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/discord.png" alt="Discord" width=64></a>
&nbsp;
<a href="https://www.linkedin.com/in/jonathanho33" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/linked_in.png" alt="LinkedIn" width=64></a>
&nbsp;
<a href="mailto:jonho.minecraft@gmail.com" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/email.png" alt="Email" width=64></a>
&nbsp;
<a href="https://www.patreon.com/minecraft_transit_railway" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/patreon.png" alt="Patreon" width=64></a>

---

## RongMC · Minecraft 26.3 Fabric 移植说明

本仓库是 RongMC 维护的 **Minecraft 26.3 / Fabric / Java 25** 移植构建，不是上游官方发行渠道。

### 与原版的主要差异
- 目标版本从文档中的 1.21.11 工作区推进到 **Minecraft 26.3**（Java 25、Loom/Fabric API 同步升级）。
- Fabric id 改为 `rongmc_mtr`，并通过 `provides: [mtr]` 兼容旧依赖声明；资源命名空间仍为 `mtr`。
- 与交通套件共用 `rongmc_mapping`（Minecraft-Mappings）适配层；构建入口在 RongMC-26.3 多工程工作区。
- 沿用并扩展既有非官方补丁方向（轨道渲染/遮挡、电梯连线、物品模型谓词、诊断快捷键等），具体以当前分支代码为准。

### 已知问题 / 限制
- 官方上游仍以自家版本线为准；本仓为非官方移植，版本号带 `port` 后缀。
- 部分客户端 GUI / 映射层 mixin 在纯服务端上会出现 “找不到客户端类” 的 Mixin 警告（可忽略）。
- 与极旧存档/旧版 MTR 数据未必完全二进制兼容，进服前请备份。
- 系统地图 HTTP（默认 :8888）依赖模组正常初始化；端口占用会导致地图服务起不来。

### 构建与安装
- Fabric mod id：`rongmc_mtr`（`provides`：`mtr`，旧依赖名一般仍可解析）
- Release 产物见本仓库 Releases（jar 名带 `rongmc-` 前缀）
- 许可证：本仓以 **GPL-3.0** 发布（见 `LICENSE`）；请同时尊重上游原作者条款与署名

