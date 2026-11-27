# 我的世界铁路 4.0.5

简体中文 · [English](./README.en.md)


我的世界铁路（Minecraft Transit Railway，MTR）让你在 Minecraft 里建造自己的公共交通系统。布设轨道、站台和侧线，设置路线与时刻表，让列车、轮船、缆车和飞机自动运行。交通模拟与 Minecraft 实例分开运行，以减轻服务端负担。

## 主要特色

- 自定义列车编组、车体和外观，车辆拥有转向架、行驶摆动及更真实的运动。
- 使用车站、线路、侧线和信号规划列车运营。
- 建造电梯、扶梯及横向运行的电梯，并搭配车站设备和屏蔽门。
- 通过在线系统地图查看线路，或接入 OneBusAway。
- 支持 OBJ 车辆资源及来自 Nemo's Transit Expansion 的部分模型能力。

[![视频预告](https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/video-preview.png)](https://www.youtube.com/watch?v=1cZfU7t4cAk)

[上游功能演示](https://www.youtube.com/playlist?list=PLp0jZgheSlXuQCXu9UynrKuBnq2Ef_TSI)

## RongMC 26.3 版本的改动

- **原生交通实体。** 列车车厢、电梯轿厢使用实体模型、位置插值与碰撞，原有交通模拟仍负责运行。支持进出停靠载具和随车移动，具体表现取决于车门和停靠配置。
- **实际轨道方块。** 兼容的旧轨道随区块加载转换为带碰撞的轨道方块，继续使用原有轨道网络与调度。
- **自定义车辆。** 通过资源包创建器导入 Blockbench、OBJ 或 MQO 模型。车门、转向架等活动部分可分组，联机客户端使用同一份资源包。
- **界面与渲染。** 适配 26.3 原版渲染与 SDL 输入。搭配 RongMC Transport UI 可在经典界面和 Oriel 界面间切换。

主分支修复了走进列车或电梯时的位置偏移。玩家脚底对齐车厢地板，走过门槛时按实际步幅离开；从列车模型库按 Esc 可返回仪表板。

## 安装

在 [GitHub Releases](https://github.com/OpenRongMC/Minecraft-Transit-Railway/releases/tag/v4.0.5-port.1%2B26.3.fix.1) 下载 **v4.0.5-port.1+26.3.fix.1**。使用 Minecraft Java **26.3**、**Fabric** 和 **Java 25**。 依赖 [Fabric API](https://modrinth.com/mod/fabric-api)。 将发行 JAR 放进实例的 `mods/` 目录。 联机时两端安装同一发布系列的 MTR 与所选交通扩展。需要 Oriel 外观时，只在客户端额外安装 RongMC Transport UI 和 Oriel。安装前先核对扩展的依赖与适用版本。

## 玩法指南

参阅 [MTR Wiki](https://wiki.minecrafttransitrailway.com/start)。如果是第一次搭线路，可以先完成「轨道 → 站台 → 侧线 → 路线」的流程，再在侧线配置发车和车型。

## 推荐扩展

- [常磐客户端模组（JCM）](https://github.com/OpenRongMC/Joban-Client-Mod)
- [伦敦地铁](https://github.com/OpenRongMC/London-Underground)
- [俄罗斯地铁](https://github.com/OpenRongMC/Russian-Metro)
- [天津地铁](https://github.com/OpenRongMC/Tianjin-Metro)
- [车站装饰（MSD）](https://github.com/OpenRongMC/MSD)
- [云竹交通扩展（YTE）](https://github.com/OpenRongMC/Yunzhu-Transit)
- [Oriel 界面适配](https://github.com/OpenRongMC/RongMC-Transport-UI)

## 参与开发

使用 Java 25，在配套的交通套件工作区与 [Minecraft Mappings](https://github.com/OpenRongMC/Minecraft-Mappings) 一起构建。翻译可参加[上游 Crowdin](https://crowdin.com/project/minecraft-transit-railway)，游戏问题可在 [RongMC Issues](https://github.com/OpenRongMC/Minecraft-Transit-Railway/issues) 反馈。

## 许可

上游项目采用 [MIT](https://opensource.org/licenses/MIT)。内置 Noto 字体使用 [Open Font License](https://openfontlicense.org/)。[上游仓库](https://github.com/jonafanho/Minecraft-Transit-Railway)。
