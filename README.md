# Extra Utilities Rebirth

Community port/rebuild of the classic **Extra Utilities** gameplay style for modern Forge.

This project is built with MCreator-generated structure plus custom Java code and targets Forge for Minecraft 1.20.1.

## Version and Compatibility

| Field | Value | Source |
|---|---|---|
| Mod ID | `extrautilitiesrebirth` | `src/main/resources/META-INF/mods.toml` |
| Display Name | `Extra Utilities Rebirth` | `src/main/resources/META-INF/mods.toml` |
| Mod and artifact version | `1.7.3` | `build.gradle` |
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
- Rainbow Generator, crafted by combining its top and bottom halves. Sneak-right-click the core with a Wrench, then right-click each of the 16 fuel generators with the same Wrench. Keep all 16 fueled and give each an energy destination so its buffer can continue to drain.

### Automation and Machines

- Mechanical Miner
- Mechanical User
- Ender Quarry + upgrades (speed/silk/world-hole style upgrade blocks)
- Quantum Quarry and actuator (legacy WIP placeholders)
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

## Setting up the Ender Quarry

Place four Ender Markers at the corners of a rectangle at the same height.
Right-click the marker that will connect to the Ender Quarry, and confirm that
the scan finds all four corners. Place the quarry next to that marker and
right-click the quarry to start it. Supply Forge Energy and place an inventory
on a free face to receive the drops.

The quarry scans the interior starting one block below the machine. Place
upgrades against a free face. Speed and Silk Touch work alongside the marker
setup. The quarry normally replaces mined blocks with dirt. The World Hole
upgrade leaves the excavation open.

The Quantum Quarry and its actuators remain work in progress, as they were in
the bundled workspace.

## Build Instructions

Run the Gradle wrapper with a Java 17 JDK.

```bash
./gradlew build
./gradlew runGameTestServer
./gradlew runGameTestServer -PminimalRuntime=true
```

The build produces `build/libs/extrautilitiesrebirth-1.7.3.jar`. The GameTest
server runs without a window and stores separate worlds for the two profiles in
`build/forge-gametest`. The minimal runtime run leaves out JEI and the optional
development mod jars.

Use `./gradlew runServer` for a dedicated development server or
`./gradlew runClient` for a development client.

To install the jar into the local PrismLauncher Auram instance, run
`./gradlew deployToModpack`. Pass `-PmodpackModsDir=/path/to/mods` to choose a
different destination. The deployment task copies the jar as `ExtraUtilities.jar`.

## Dependencies and Integrations

- Core dependency: Forge `1.20.1-47.4.10`.
- Runtime local mod jars are loaded from `extra-mods-1.20.1/` as `runtimeOnly` deobf dependencies.
- JEI APIs are included as compile/runtime integration dependencies.
- Optional dependencies declared in `mods.toml`:
  - `allmods` (optional)
  - `jei` (optional)

## Project Notes

- This is a port/recreation project and not a byte-for-byte reimplementation of legacy Extra Utilities code.
- `extrautilitiesrebirth.mcreator` still reports `currentGenerator: forge-1.18.2`, while active Gradle build targets 1.20.1.
- License is declared as MIT in metadata, but there is no standalone `LICENSE` file in the repository root.
