# Extra Utilities Rebirth

Community port/rebuild of the classic **Extra Utilities** gameplay style for modern Forge.

This project is built with MCreator-generated structure plus custom Java code and targets Forge for Minecraft 1.20.1.

## Version and Compatibility

| Field | Value | Source |
|---|---|---|
| Mod ID | `extrautilitiesrebirth` | `src/main/resources/META-INF/mods.toml` |
| Display Name | `Extra Utilities Rebirth` | `src/main/resources/META-INF/mods.toml` |
| Mod Version (metadata) | `1.7.3` | `src/main/resources/META-INF/mods.toml` |
| Project Artifact Version | `1.0` | `build.gradle` |
| Minecraft Version | `1.20.1` | `build.gradle` / `mods.toml` |
| Forge Version | `1.20.1-47.4.10` | `build.gradle` |
| Java Version | `17` | `build.gradle` |
| Gradle Distribution | `8.8` | `gradle/wrapper/gradle-wrapper.properties` |
| Declared License | `MIT License` | `mods.toml` / `extrautilitiesrebirth.mcreator` |

## What This Mod Adds

This port includes a large set of utility-focused content inspired by older Extra Utilities releases:

- Compressed blocks (cobble/dirt/sand/gravel tiers, up to octuple for cobblestone).
- Utility and special-purpose glass variants (ethereal, ineffable, dark, reinforced, decorative sets).
- Generator progression and specialty generators.
- Automation machines and quarry systems.
- Storage/handling blocks (drums, chests, trash cans, inventory/transfer utilities).
- Utility tools and items (rings, bag of holding, lasso, upgrades, ingots, machine parts).
- Ender-themed progression (Ender Quarry, Ender Thermic Pump, Ender Lily chain, Ender items).

### Content Scope (Current Source Tree)

- `130` registered blocks.
- `170` total registered items (`130` block items + `40` standalone items).
- `46` block entities.
- `12` GUI menus/screens.
- `155` crafting recipes.
- End worldgen integration for Ender Lily Endstone (configured + placed feature + biome modifier).

## Feature Highlights

### Power and Generators

Includes many generator types such as:

- Furnace, Culinary, Ender, TNT, Frosty, Halitosis, Netherstar.
- Overclocked, Pink, Potion, Slimey, Survival, Redstone, Magma.
- Death and Disenchantment generators.
- Rainbow generator multiblock-style structure (top/bottom/core parts present).

### Automation and Machines

- Mechanical Miner
- Mechanical User
- Ender Quarry + upgrades (speed/silk/world-hole style upgrade blocks)
- Quantum Quarry + actuator
- Ender Thermic Pump
- Sorting Machine
- Inventory Interface
- Resonator / machine component chain

### Storage and Utility

- Gold/Creative chests
- Drums (Stone, Iron, Reinforced, Bedrock, Creative)
- Trash Cans (item, energy, fluid)
- Wireless FE Battery / FE Transmitter blocks
- Redstone clock variants

### Tools, Mobility, and Utility Items

- Kikoku, Fire Axe, Healing Axe, Erosion Shovel, Destruction Pickaxe, Etheric Sword
- Golden Bag of Holding
- Golden Lasso
- Angel Ring / Chicken Wing Ring upgrades
- Speed upgrade tiers and machine upgrade items

### Worldgen

- Ender Lily Endstone generation in End biomes via Forge biome modifier:
  - biome tag: `#minecraft:is_end`
  - placement count/height data under:
    - `src/main/resources/data/extrautilitiesrebirth/worldgen/configured_feature/ender_lilly_endstone.json`
    - `src/main/resources/data/extrautilitiesrebirth/worldgen/placed_feature/ender_lilly_endstone.json`

## Repository Layout

- `src/main/java` - mod code (registries, blocks/items/entities, GUIs, procedures).
- `src/main/resources` - assets, recipes, tags, worldgen, `mods.toml`.
- `elements/` - MCreator element definitions.
- `extrautilitiesrebirth.mcreator` - MCreator workspace metadata.
- `extra-mods-1.20.1/` - local jar dependencies loaded as runtime mods for dev.
- `models/` - model source assets used by elements/resources.

## Build Instructions

### Prerequisites

- Java 17 JDK installed and available in `PATH`.
- Gradle 8.8+ installed locally, or a valid Gradle wrapper jar.

### Important Build Caveats in This Repository

1. `gradle/wrapper/gradle-wrapper.jar` is currently empty in this checkout, so `./gradlew` will fail until the wrapper jar is restored/regenerated.
2. `build.gradle` adds `build.finalizedBy deployToModpack`, which copies the built jar to a local PrismLauncher path:
   - `/Users/cyberpwn/Library/Application Support/PrismLauncher/instances/Auram/minecraft/mods/ExtraUtilities.jar`
3. To avoid the machine-specific deploy copy step, build with `-x deployToModpack`.

### Build Commands

If you have system Gradle installed:

```bash
gradle clean build -x deployToModpack
```

For development runs:

```bash
gradle runClient
gradle runServer
```

If you want to restore wrapper usage:

```bash
gradle wrapper --gradle-version 8.8
chmod +x gradlew
./gradlew clean build -x deployToModpack
```

### Build Outputs

- Main jar: `build/libs/extrautilitiesrebirth-1.0.jar`
- Reobfuscated output: `build/reobfJar/output.jar`
- Optional deployed copy task output name: `ExtraUtilities.jar`

## Dependencies and Integrations

- Core dependency: Forge `1.20.1-47.4.10`.
- Runtime local mod jars are loaded from `extra-mods-1.20.1/` as `runtimeOnly` deobf dependencies.
- JEI APIs are included as compile/runtime integration dependencies.
- Optional dependencies declared in `mods.toml`:
  - `allmods` (optional)
  - `jei` (optional)

## Project Notes

- This is a port/recreation project and not a byte-for-byte reimplementation of legacy Extra Utilities code.
- Metadata currently has version mismatch:
  - `mods.toml`: `1.7.3`
  - `build.gradle`: `1.0`
- `extrautilitiesrebirth.mcreator` still reports `currentGenerator: forge-1.18.2`, while active Gradle build targets 1.20.1.
- License is declared as MIT in metadata, but there is no standalone `LICENSE` file in the repository root.
