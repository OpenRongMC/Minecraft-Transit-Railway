# 我的世界铁路（MTR）

简体中文 · [English](./README.en.md)

在 Minecraft 里建造并调度公共交通。用轨道、站台、侧线、线路和时刻表组织列车运行，也可布置电梯、扶梯和站内设施。RongMC 26.3 移植保留 MTR 4.0.5 的交通模拟，适配了原版渲染提交、轨道方块和原生载具实体。

## 下载与安装

- 版本 · [v4.0.5-port.1+26.3](https://github.com/OpenRongMC/Minecraft-Transit-Railway/releases/tag/v4.0.5-port.1%2B26.3) (已发布)
- 下载文件 · `rongmc-mtr-4.0.5-port.1.26.3.jar`
- 运行环境 · Minecraft Java 26.3, Fabric, Java 25
- Fabric Loader · `>=0.19.3`
- Fabric 模组 ID · `rongmc_mtr`
- JAR 声明的依赖 · [Fabric API](https://modrinth.com/mod/fabric-api)

把发行 JAR 放入对应实例的 `mods/` 目录。联机时两端安装同一发布系列的 MTR 与所选交通扩展。需要 Oriel 外观时，只在客户端额外安装 RongMC Transport UI 和 Oriel。安装前先核对扩展的依赖与适用版本。

## 如何验证

在新创意世界先连接一段轨道、站台和侧线，配置一条线路，确认列车能发车和停靠。旧世界应先复制，重点检查道岔、站台门、列车和电梯。自定义列车经资源包创建器导入 Blockbench、OBJ 或 MQO 模型。联机需让玩家使用相同资源包。

## 旧世界与自定义列车

旧轨道数据在兼容的区块加载后可转换成实际轨道方块。列车仍由原路线和时刻表驱动，载具的模型和碰撞则走新版实体路径。不同车型和站台配置的载客行为需结合现有存档实测。

导入时将车门、转向架等活动部件分组，导出后在资源包界面启用。用同一车辆 ID 替换现有车型外观时，先在编组副本上验证车门分组和材质。

发行 JAR 已内嵌所需映射层。一般只需安装 MTR 发行 JAR，再按所选扩展补齐例如 Filters API 的依赖。

## 从源码开始

```sh
./gradlew tasks
```

从开发所用的交通套件根目录联动构建。单仓保留历史平台目录，不能仅凭其中的旧 Gradle 配置重建发布 JAR。

## 文档与反馈

- [原 README（旧版指南、贡献者与署名）](https://github.com/OpenRongMC/Minecraft-Transit-Railway/blob/9f150720a4b818ff789b167931791d8b4849c4ed/README.md)
- [上游项目与文档](https://github.com/jonafanho/Minecraft-Transit-Railway)
- [创造栏筛选 API](https://github.com/OpenRongMC/Filters-API)
- [MTR 平台映射与兼容层](https://github.com/OpenRongMC/Minecraft-Mappings)
- [常磐客户端模组（JCM）](https://github.com/OpenRongMC/Joban-Client-Mod)
- [伦敦地铁扩展](https://github.com/OpenRongMC/London-Underground)
- [俄罗斯地铁扩展](https://github.com/OpenRongMC/Russian-Metro)
- [天津地铁扩展](https://github.com/OpenRongMC/Tianjin-Metro)
- [MTR 车站装饰扩展（MSD）](https://github.com/OpenRongMC/MSD)
- [云竹交通扩展（YTE）](https://github.com/OpenRongMC/Yunzhu-Transit)
- [RongMC 交通界面](https://github.com/OpenRongMC/RongMC-Transport-UI)
- [报告 RongMC 移植问题](https://github.com/OpenRongMC/Minecraft-Transit-Railway/issues) · 请附游戏版本、两端 JAR 文件名、复现步骤和相关日志。

## 许可与署名

请参阅本仓库的 [LICENSE](./LICENSE) 及原项目的许可、署名和第三方资源说明。
