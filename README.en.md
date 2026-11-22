# Minecraft Transit Railway (MTR)

[简体中文](./README.md) · English

Build and schedule public transit in Minecraft. Lay tracks, define platforms and sidings, then configure routes and timetables. The RongMC port retains the MTR 4.0.5 simulation and adapts it to 26.3 rendering, physical track blocks, and native transport entities.

## Download and installation

- Version · [v4.0.5-port.1+26.3](https://github.com/OpenRongMC/Minecraft-Transit-Railway/releases/tag/v4.0.5-port.1%2B26.3) (Release)
- Release asset · `rongmc-mtr-4.0.5-port.1.26.3.jar`
- Runtime · Minecraft Java 26.3, Fabric, Java 25
- Fabric Loader · `>=0.19.3`
- Fabric mod ID · `rongmc_mtr`
- Dependencies declared by the JAR · [Fabric API](https://modrinth.com/mod/fabric-api)

Place the release JAR in the instance’s `mods/` directory. Use the same release family of MTR and your chosen addons on both sides of a multiplayer installation. For the Oriel appearance, install RongMC Transport UI and Oriel on the client. Check the dependency ranges for any addons you select.

## How to try it

In a fresh creative world, connect a short track, platform and siding, then schedule one route and confirm the train departs and stops. Test junctions, platform doors, trains and lifts on a copy of any existing world. Import Blockbench, OBJ or MQO models through the resource-pack creator and distribute the same resource pack to multiplayer clients.

## Older worlds and custom vehicles

Legacy rail data can convert into physical track blocks when compatible chunks load. Routes and timetables still drive the services, while vehicle models and collision use the native entity path. Verify boarding and onboard movement with the vehicles and platforms in your own save.

Group moving parts such as doors and bogies before exporting, then enable the pack in Minecraft. When replacing an existing vehicle’s appearance under the same ID, validate door groups and materials on a test consist.

The release JAR already bundles the matching mapping layer. Install the MTR release JAR and then add dependencies such as Filters API if your chosen addons require them.

## Working from source

```sh
./gradlew tasks
```

Build with the coordinated transport workspace. The standalone repository retains historical platform files, so those Gradle settings alone do not recreate the published JAR.

## Documentation and support

- [Previous README (historical guide, contributors and credits)](https://github.com/OpenRongMC/Minecraft-Transit-Railway/blob/9f150720a4b818ff789b167931791d8b4849c4ed/README.md)
- [Upstream project and documentation](https://github.com/jonafanho/Minecraft-Transit-Railway)
- [Filters API](https://github.com/OpenRongMC/Filters-API)
- [MTR Platform Mappings and Compatibility](https://github.com/OpenRongMC/Minecraft-Mappings)
- [Joban Client Mod (JCM)](https://github.com/OpenRongMC/Joban-Client-Mod)
- [London Underground Addon](https://github.com/OpenRongMC/London-Underground)
- [Russian Metro Addon](https://github.com/OpenRongMC/Russian-Metro)
- [Tianjin Metro Addon](https://github.com/OpenRongMC/Tianjin-Metro)
- [MTR Station Decoration Addon (MSD)](https://github.com/OpenRongMC/MSD)
- [Yunzhu Transit Extension (YTE)](https://github.com/OpenRongMC/Yunzhu-Transit)
- [RongMC Transport UI](https://github.com/OpenRongMC/RongMC-Transport-UI)
- [Report a RongMC port issue](https://github.com/OpenRongMC/Minecraft-Transit-Railway/issues) · Include the game version, client and server JAR names, reproduction steps and relevant logs.

## License and credits

See this repository’s [LICENSE](./LICENSE) and the original project’s license, credits and third-party asset notices.
