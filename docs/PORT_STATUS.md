# Forge 1.20.1 port checklist

Use the active Java and resource registries for the scope of this port. The
bundled 1.18.2 workspace and the MCreator elements give context for feature
behavior. Consult [RWTema’s Extra Utilities 2 source](https://github.com/rwtema/Extra-Utilities-2-Source)
for feature intent where the bundled workspace is incomplete. A checked box
requires gameplay or test evidence for the whole entry.
Block items follow the entry for the block that registers them.

## Gameplay

| Status | Feature group | Coverage and current evidence |
| --- | --- | --- |
| [ ] | Compressed materials | Every cobblestone, dirt, sand, and gravel tier. Verify recipes in both directions and break/drop behavior. |
| [ ] | Building materials | Bedrock bricks, cobblestone and slabs, border stone, chiseled and edged stone bricks, polished stone, quartzburnt, rainbow stone, stoneburnt, magical wood and planks, ferrous juniper log and planks. Check crafting, placement, rotation, and drops. |
| [ ] | Glass | Sandy, creeper, dark ethereal, dark, edged, ethereal, clean, bricks, glowstone, golden edged, heart, ineffable, ineffable inverted, obsidian, reinforced dark, square, and swirling. Check appearance and each variant's light, collision, and harvesting rules. |
| [ ] | Materials and components | Demon, enchanted and evil infused metal blocks, ender infused obsidian, ender core, machine block, diamond edged matrix, upgrade base, lunar reactive dust, red coal, enchanted ingot, bedrockium ingot, unstable ingot and nugget, evil infused iron ingot, demon ingot, eye of redstone, moon stone, redstone crystal, ender shard, and amazing opinium core. Trace crafting or acquisition through each consumer. |
| [ ] | Placement and access | Angel Block, Chandelier, Chunk Loading Wand, Overworld Portal, and Portable Globe. Verify placement and activation in the intended dimensions, plus chunk and portal behavior after reload. |
| [ ] | Spikes and Cursed Earth | Wood, stone, iron, gold, diamond, and creative spikes, Cursed Earth, and Drop of Evil. Verify damage, mob spawning, drops, conversion, and light behavior. |
| [ ] | Ender Lilies | Endstone feature and all eight stages. Verify End world generation, placement, growth, harvesting, seed drops, and collision. |
| [ ] | Item and fluid storage | Gold Chest, Creative Chest, Golden Bag of Holding, and stone, iron, reinforced, bedrock, and creative drums. The baseline Gold Chest retained items in slots 0 and 53 across a server restart. Verify slot and tank limits, automation, and all storage actions. |
| [ ] | Disposal and creative utilities | Item, energy, and fluid Trash Cans, Creative Energy Storage, Creative Harvest, and Creative Inventory Sample. Verify filters, limits, and creative access. |
| [ ] | Energy distribution | Solar Panel, Lunar Panel, Wireless FE Battery, FE Transmitter, and Wireless RF Heating Coil. Verify generation windows, connection persistence, transfer and receiver behavior, and wire or wireless interoperability. |
| [ ] | Redstone Clock | Both states. Verify placement, pulse timing, redstone and comparator updates, and chunk and server reload. |
| [ ] | Fuel generators | Culinary, Death, Disenchantment, Ender, TNT, Frosty, Furnace, Halitosis, Netherstar, Overclocked, Pink, Potion, Slimey, Survival, Redstone, and Magma, plus the three speed upgrades. TNT GameTests cover generation, status, all six output sides, a full receiver, and redstone control through the menu action. The Pink and Frosty fuel tags load and are covered by GameTests. Still verify the other fuel tables, remainders, upgrade rates, and full burn behavior and effects across the family. |
| [ ] | Rainbow Generator | Core, top, and bottom. Verify assembly, linked generator activity, saved links, output, and power remaining after the multiblock stops. |
| [ ] | Ender Quarry | Quarry, marker, base upgrade, speed upgrade, world hole upgrade, and each Silk Touch tier. GameTests cover blocked output/resume, tags, sided insertion, multi-drop serialization, and dirt replacement. Verify normal marker setup, traversal, upgrades, and chunk and server reload. |
| [ ] | Quantum Quarry | Core and six actuators have survival recipes. GameTests cover FE through assembled actuators, mining in a separate generated dimension, blocked output and resumed export, persistent terrain cursor, and the mining book menu slot. Verify client layout and gameplay in an upgraded save. |
| [ ] | Ender Thermic Pump | Verify fluid output, use of FE, range, replacement behavior, and persistence. |
| [ ] | Automation and production | Mechanical Miner, Mechanical User, Furnace, Sorting Machine, Enchanter, Resonator, and Inventory Interface. Verify ordinary inputs and outputs, sided automation, blocked destinations, controls, and recipes. |
| [ ] | Tools | Glass Cutter, Reinforced Glass Cutter, Wrench, Fire Axe, Kikoku, Healing Axe, Erosion Shovel, Precision Shears, Reversing Hoe, Etheric Sword, and Destruction Pickaxe. Verify tool actions, durability, drops, and client/server behavior. |
| [ ] | Mob capture | Golden Lasso. GameTests cover animal and villager capture, stack splitting, serialization, blocked release, hostile rejection, and crafting safety. Verify real client interactions. |
| [ ] | Player utilities | Division Sigil, Magical Apple, Golden Feather, Chicken Wing Ring, Advanced Chicken Wing Ring, Angel Ring, and Ring of the Flying Squid. Compare intended actions and progression to the workspace. Review the Angel Ring's external integration. |

## Menus and runs

- [ ] Verify all 12 registered menus in a muted client on an isolated display.
  Check Creative Chest, generator, Mechanical Miner, Mechanical User,
  Resonator, Furnace, Enchanter, Trash Can, Golden Bag, Gold Chest, Ender Quarry,
  and Sorter. Corrected background sizes for the six affected screens. Include
  clicks, shift-clicks, menu validity, persistence, and server synchronization.
- [ ] Walk crafting progression through all gameplay systems. Check unresolved
  item tags, recipes, assets, and world generation data at startup and reload.
- [x] Restore Gradle 8.8 wrapper, align the jar and mod versions, and make
  modpack deployment explicit. Built 1.7.3 with Java 17. The explicit deploy
  copy matched the jar when pointed at a temporary destination.
- [x] Run the current GameTest suite on Forge 47.4.10 with and without the
  optional development mods. The 15 quarry, generator, and lasso tests passed in
  both configurations. Rerun as coverage grows.
- [ ] Build the final jar from a clean checkout and load that packaged jar in
  both the client and dedicated server. The packaged build at `697949b` started
  on a fresh Forge 47.4.10 dedicated server installation.
- [ ] Upgrade an isolated copy of a representative 1.20.1 save. Check registry
  IDs, inventory contents, FE buffers, fuel and machine state, then restart and
  check again. The packaged build at `697949b` preserved sampled Gold Chest
  slots, TNT fuel and FE, redstone mode, and named item data across upgrade and
  restart. The previously idle east receiver was powered after the upgrade.
