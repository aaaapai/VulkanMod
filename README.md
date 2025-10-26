# <a href="https://github.com/xCollateral/VulkanMod"> <img src="./src/main/resources/assets/vulkanmod/Vlogo.png" width="30" height="30"/> </a> VulkanMod

Bring Vulkan 1.2 rendering to Minecraft Java for smoother visuals, lower CPU overhead, and access to modern GPU
features.

---

### Downloads

- [![CurseForge](https://cf.way2muchnoise.eu/full_635429_downloads.svg?badge_style=flat)](https://www.curseforge.com/minecraft/mc-mods/vulkanmod)
- [![Modrinth Downloads](https://img.shields.io/modrinth/dt/JYQhtZtO?logo=modrinth&label=Modrinth%20Downloads)](https://modrinth.com/mod/vulkanmod/versions)
- [![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/xCollateral/VulkanMod/total?style=flat-square&logo=github&label=Github%20Downloads)](https://github.com/xCollateral/VulkanMod/releases)

---

## Overview

VulkanMod is an experimental Fabric client mod that replaces Minecraft Java's OpenGL 3.2 renderer with a custom Vulkan
1.2 pipeline. The goal is to modernize the engine, reduce CPU overhead, and unlock rendering features that are difficult
or impossible with OpenGL.

### Highlights

- Rewritten chunk renderer with multiple culling algorithms, indirect draws, and tuned upload pipelines.
- Native Wayland support, GPU selector, and revamped video settings tailored for Vulkan.
- Reduced CPU overhead with multithreaded region builders and efficient buffer management.
- Acts as a living reference for building a Vulkan renderer on top of Minecraft's client.

### Demo

[![Demonstration Video](http://img.youtube.com/vi/sbr7UxcAmOE/0.jpg)](https://youtu.be/sbr7UxcAmOE)

## Compatibility & Requirements

- Minecraft: `1.21`, `1.21.1`, `1.21.10`
- Fabric Loader: `0.14.14` or newer
- Fabric API: bundled modules specified in `build.gradle`
- Java: 21 (matching the Gradle toolchain)
- GPU/Driver: Vulkan 1.2 capable device (MoltenVK is bundled for macOS)

## Installation

> Always back up your saves before testing experimental client mods.

1. Install the [Fabric Mod Loader](https://fabricmc.net) for your Minecraft version.
2. Download the latest `VulkanMod` release from Modrinth, CurseForge, or GitHub.
3. Drop the `.jar` into your `.minecraft/mods` directory.
4. Launch Minecraft with the Fabric profile and configure options in the in-game settings menu.

For support, read the [Wiki](https://github.com/xCollateral/VulkanMod/wiki) first, then visit
the [Discord server](https://discord.gg/FVXg7AYR2Q) or [open an issue](https://github.com/xCollateral/VulkanMod/issues)
with full logs.

## Feature Roadmap

- [x] Multiple chunk culling strategies
- [x] Indirect draw submission path
- [x] Resizable frame queue and windowed fullscreen
- [x] Native Wayland surface support
- [x] GPU selection UI
- [ ] User-exposed shader support
- [ ] Heavily requested nostalgia feature: Removed Herobrine

## Project Notes

- VulkanMod is a full renderer rewrite, not a translation layer
  like [Mesa's Zink](https://docs.mesa3d.org/drivers/zink.html).
- Expect rapid iteration; breaking changes may occur. Share logs when reporting issues so we can reproduce them quickly.

## Building From Source

Use the bundled Gradle wrapper in the repository root:

```bash
./gradlew build          # compile, remap, and package the mod
./gradlew runClient      # launch a dev client for smoke tests
./gradlew genSource      # refresh decompiled sources after mapping updates
```

- Redirect verbose commands to a log (for example `./gradlew genSource > genSource.log`) if you need to inspect output
  later.
- Fabric Loom is configured to use official Mojang mappings; the access widener stays in the `named` namespace.

## Contributing

- Read open issues and roadmap discussions before implementing large features.
- Follow the automation checklist in `AGENTS.md` for scripted or AI-assisted workflows.
- Keep performance-sensitive code allocations low and reuse existing utility classes.
- Submit reproducible test cases or screenshots when fixing rendering bugs.

## Community & Support

- **Wiki**: project documentation and troubleshooting guides
- **Discord**: `https://discord.gg/FVXg7AYR2Q`
- **Ko-fi**: `https://ko-fi.com/V7V7CHHJV`
- **Issues**: `https://github.com/xCollateral/VulkanMod/issues`

Thanks for experimenting with VulkanMod and helping push Minecraft's renderer forward!
