# VulkanMod Agent Handbook

This guide outlines expectations for automated contributors (AI agents and scripts) working in the VulkanMod repository. Use it to stay oriented, make safe changes, and validate your work before handing results back to maintainers.

## 1. Project Orientation
- **Purpose**: Replaces Minecraft Java's fixed-function OpenGL renderer with a modern Vulkan 1.2 pipeline for better performance and flexibility.
- **Primary code**: `src/main/java/net/vulkanmod` (rendering pipeline, Vulkan backend, mixins).
- **Assets & metadata**: `src/main/resources` (`fabric.mod.json`, access widener, textures and shaders).
- **Build tooling**: Gradle with Fabric Loom (`build.gradle`, `gradle.properties`). Java 21 is required.

## 2. First Steps for Any Task
- Read the active issue or request carefully and restate the deliverable in your own words.
- Scan existing changes in the worktree (`git status`, `git diff`) so you do not discard user edits.
- Locate relevant code with `rg` or IDE navigation before writing anything. Rendering logic is split into `render`, `vulkan`, `interfaces`, and mixin subpackages.
- Prefer targeted edits; avoid broad refactors unless directly requested.

## 3. Coding Guidelines
- Keep the mod compatible with the Minecraft and Fabric versions listed in `gradle.properties`. Do not assume APIs from newer versions without guard rails.
- Rendering code is performance critical. Avoid allocations in hot loops and favor pre-sized buffers (`StaticQueue`, `UploadBuffer`, etc.).
- Reuse existing logging via `Initializer.LOGGER`; only log at info or warn levels unless debugging.
- When touching mixins, confirm the injection points against current Mojang mappings. The expected source names come from official Mojang mappings (Loom configuration).
- For concurrency (chunk builders, queues), review thread-safety assumptions in `TaskDispatcher`, `ThreadBuilderPack`, and related classes before changing shared state.

## 4. Build & Test Commands
- `./gradlew build` – default validation (unit compilation, remapping, JAR). Run after substantive code changes.
- `./gradlew runClient` – launch a dev client if you need runtime smoke tests (requires a local Minecraft installation).
- `./gradlew genSource` – refresh decompiled sources after mapping updates (mentioned in `README.md`; rarely needed otherwise).
- Capture logs: redirect noisy Gradle commands (`./gradlew genSource > genSource.log`) if you need to inspect output post-run.

## 5. Documentation & Assets
- Update `README.md` for user-facing changes (new features, install steps). Keep badges and download links intact.
- Image assets live under `src/main/resources/assets/vulkanmod`. Do not change resolutions without checking UI code.
- When adding config options, document defaults in both the README and `net.vulkanmod.config` package comments if relevant.

## 6. Validation Checklist Before Returning Work
- Java compiles locally (`./gradlew build` succeeds) or you documented why it could not be run.
- All touched files use consistent formatting (Gradle-managed; no reflow tool needed).
- No secrets or personal data added to commits.
- README and docs reference new features or breaking changes when applicable.
- Summaries clearly state what changed, why, and how to verify it.

## 7. Useful References
- `build.gradle`: shows included Fabric modules and LWJGL/VMA dependencies.
- `fabric.mod.json`: authoritative metadata (mod ID, supported MC versions).
- `net.vulkanmod.render.*`: rendering pipeline entry points (`WorldRenderer`, `SectionGraph`, chunk builders).
- `net.vulkanmod.vulkan.*`: Vulkan device, queues, memory allocators.
- `net.vulkanmod.config.*`: runtime settings surfaced to users.

Act conservatively, communicate uncertainties, and prefer incremental patches. When in doubt, ask for clarification before proceeding.
