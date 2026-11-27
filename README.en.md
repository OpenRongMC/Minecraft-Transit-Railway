# Minecraft Transit Railway 4.0.5

[简体中文](./README.md) · English


_Minecraft Transit Railway_ is a [Minecraft mod](https://minecraft.wiki/w/Mods) that allows you to build your own transport network with automated trains, boats, cable cars, and planes!

This is the RongMC Fabric port for Minecraft 26.3. The vehicle simulation code is separated from the Minecraft instance, reducing lag on Minecraft servers.

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

## What changed in the RongMC 26.3 port

- **Native transport entities.** Train cars and lift cabins use entity models, position interpolation and collision while the existing simulation runs the services. Boarding and movement depend on door and stop configuration.
- **Physical track blocks.** Compatible legacy rails convert as chunks load, with collision and the existing rail network and schedules retained.
- **Custom vehicles.** Import Blockbench, OBJ or MQO models in the resource-pack creator. Group moving parts such as doors and bogies, and distribute the same pack to multiplayer clients.
- **Interface and rendering.** Rendering and SDL input use the 26.3 paths. RongMC Transport UI adds an optional switch between Classic and Oriel.

The main branch aligns riders’ feet with the cabin floor and preserves their walking step as they cross a train or lift doorway. Pressing Esc in the Train Models library returns to the dashboard.

## Installation

Download **v4.0.5-port.1+26.3.fix.1** from [GitHub Releases](https://github.com/OpenRongMC/Minecraft-Transit-Railway/releases/tag/v4.0.5-port.1%2B26.3.fix.1) for Minecraft Java **26.3** on **Fabric** with **Java 25**. Dependencies include [Fabric API](https://modrinth.com/mod/fabric-api). Place the release JAR in the instance’s `mods/` directory. Use the same release family of MTR and your chosen addons on both sides of a multiplayer installation. For the Oriel appearance, install RongMC Transport UI and Oriel on the client. Check the dependency ranges for any addons you select.

Upstream releases and project information are available on [Modrinth](https://modrinth.com/mod/minecraft-transit-railway).

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

Use Java 25 and build MTR with the matching 26.3 transport workspace and [Minecraft Mappings](https://github.com/OpenRongMC/Minecraft-Mappings). The workspace also builds the web-based model creator.

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
