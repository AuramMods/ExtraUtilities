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
| [x] | Compressed materials | GameTests craft, unpack, place, and break all eight cobblestone tiers, four dirt tiers, two sand tiers, and two gravel tiers. Unpacking the first dirt tier returns nine ordinary dirt blocks. |
| [x] | Building materials | GameTests exercise the bedrockium, decorative stone, and Magical Wood crafting paths, including the bedrockium conversions. They cover placement, applicable rotation, and drops for all fourteen building blocks. Processor tests cover Quartzburnt, Rainbow Stone, and Stoneburnt. Magical Wood supplies its enchantment bonus, and Ferrous Juniper uses a stone axe or better. |
| [x] | Glass | GameTests cover light blocking, light emission, matching-face culling, and drops for all 17 variants. Ethereal, Dark Ethereal, and Ineffable Glass let a standing player pass through and stop mobs and sneaking players. Inverted Ineffable Glass reverses those rules. |
| [ ] | Materials and components | Demon, enchanted and evil infused metal blocks, ender infused obsidian, ender core, machine block, diamond edged matrix, upgrade base, lunar reactive dust, red coal, enchanted ingot, bedrockium ingot, unstable ingot and nugget, evil infused iron ingot, demon ingot, eye of redstone, moon stone, redstone crystal, ender shard, and amazing opinium core. Trace crafting or acquisition through each consumer. |
| [ ] | Placement and access | Angel Block, Chandelier, Chunk Loading Wand, Overworld Portal, and Portable Globe. Verify placement and activation in the intended dimensions, plus chunk and portal behavior after reload. |
| [ ] | Spikes and Cursed Earth | Wood, stone, iron, gold, diamond, and creative spikes, Cursed Earth, and Drop of Evil. Verify damage, mob spawning, drops, conversion, and light behavior. |
| [x] | Ender Lilies | The packaged dedicated server generated Ender Lily Endstone in new End chunks. GameTests cover seed drops from the ore, planting on End Stone or Ender Core, all eight stages, faster growth on Ender Core, and ripe contact damage. Picking the fruit yields one pearl and resets the plant. Breaking a ripe plant or its support yields one seed and one pearl. |
| [x] | Item and fluid storage | Gold Chest and Golden Bag tests cover 54-slot capacity, item data, and menu validity. The bag protects the held stack, rejects another bag, and shares its inventory in multiplayer item packets. The chest retained both sampled slots on an upgraded dedicated server after restart. Creative Chest tests cover item data, legal stack sizes, template protection and reload. Drum tests cover all four finite capacities, sided automation, comparators, bucket transfers, and fluid retained when moved. A creative player can configure the Creative Drum to supply an unlimited amount of its sample fluid. |
| [x] | Disposal and creative utilities | Trash Can GameTests cover all three variants, including item menu transfers, fluid buckets, sided automation, and input-only capabilities. The muted client confirmed item shift-click disposal and that the Fluid Trash Can returned the empty bucket. Creative Energy Storage supplies unlimited FE and charges machines on all six sides. Creative Harvest stays in place on an ordinary creative break and drops one copy when broken while sneaking. The Creative Inventory Sample places and drops as a block and supplies the creative tab icon. |
| [x] | Energy distribution | Solar and Lunar Panels generate 10 FE/t by day and 15 FE/t by night, respectively, with exposure to the sky. Tests cover all generation windows, delivery of legacy stored FE by a covered panel, and wired access to the FE Transmitter's 3,200 FE buffer. Wireless tests cover Wrench selection at Y=0, unlinking, receiver range and limits, and resuming loaded links. An upgraded server delivered previously stranded FE and retained the links across restart. The Wireless RF Heating Coil is a Resonator output made from iron bars using 16,000 FE. |
| [x] | Redstone Clock | Both registered states alternate every 18 ticks. GameTests cover adjacent comparator output and restoration of the saved phase. An upgraded dedicated-server save retained the clock's data and its comparator cycled between 0 and 15 after restart. |
| [ ] | Fuel generators | Culinary, Death, Disenchantment, Ender, TNT, Frosty, Furnace, Halitosis, Netherstar, Overclocked, Pink, Potion, Slimey, Survival, Redstone, and Magma, plus the three speed upgrades. GameTests cover fuel and production for each generator, the TNT menu and six output sides, full storage, serialized Culinary and Magma burns, and bucket and bottle remainders. Magma also covers bucket and pipe filling, slot validation, and exact per-bucket yield with speed upgrades. Continue checking alternate fuel variants, the full yield at each speed tier, and linked Rainbow Generator operation. |
| [x] | Rainbow Generator | GameTests craft both halves and the core, use the Wrench to select the core and link all 16 fuel generators, reload those links, and verify generation only while all 16 are producing. Stored power still transfers while the core is idle, and excess stays buffered. |
| [x] | Ender Quarry | GameTests cover four-marker setup, adjacent Speed and Silk Touch, World Hole traversal, bounded mining, blocked output and resume, sided insertion, and multi-drop serialization. The packaged client linked four markers and started a quarry with Speed and Silk Touch. That save retained the marker link, active quarry, upgrades, and intact ore output across a dedicated-server upgrade and restart. Mining resumed from the saved position. |
| [x] | Quantum Quarry placeholder | The bundled workspace marks the quarry and actuator as WIP. Preserve their existing IDs, assembly check, and menu. Mining and new recipes are outside this port's scope. |
| [x] | Ender Thermic Pump | GameTests cover normal marker setup, scanning the full rectangle below zero, leaving waterlogged or outside blocks alone, energy use, both replacement modes, partial acceptance by a drum, and resuming with a saved full tank. |
| [x] | Automation and production | Mechanical Miner, Mechanical User, and Sorter GameTests cover their inputs, modes, outputs, and saved state. Powered processor tests cover Furnace recipe counts and item data, exact FE, blocked or named output, all seven Resonator recipes, all nine Enchanter recipes, and resuming legacy work in progress. The three production menus were exercised in the muted client. Inventory Interface tests verify the bundled ten-slot automation buffer, vanilla hopper transfer, comparator output, and the iron pickaxe requirement. |
| [ ] | Tools | Glass Cutter, Reinforced Glass Cutter, Wrench, Fire Axe, Kikoku, Healing Axe, Erosion Shovel, Precision Shears, Reversing Hoe, Etheric Sword, and Destruction Pickaxe. Verify tool actions, durability, drops, and client/server behavior. |
| [ ] | Mob capture | Golden Lasso. GameTests cover animal and villager capture, stack splitting, serialization, blocked release, hostile rejection, and crafting safety. Verify real client interactions. |
| [ ] | Player utilities | Division Sigil, Magical Apple, Golden Feather, Chicken Wing Ring, Advanced Chicken Wing Ring, Angel Ring, and Ring of the Flying Squid. Compare intended actions and progression to the workspace. Review the Angel Ring's external integration. |

## Menus and runs

- [ ] Verify all 12 registered menus in a muted client on an isolated display.
  Check Creative Chest, generator, Mechanical Miner, Mechanical User,
  Resonator, Furnace, Enchanter, Trash Can, Golden Bag, Gold Chest, Sorter, and
  the Quantum Quarry placeholder served by the `ender_quarry_gui` menu type.
  Corrected background sizes for the six affected screens. Include
  clicks, shift-clicks, menu validity, persistence, and server synchronization.
  An isolated client with master volume set to zero confirmed all 12 layouts.
  Gold Chest and Golden Bag retained mouse transfers after closing and reopening.
  The TNT Generator displayed live energy, output, and burn time and responded
  to changes in redstone mode.
  A second muted client run confirmed the Mechanical Miner routed upgrades
  and ordinary items to the correct slots and displayed a full energy bar.
  The Mechanical User's button cycled through the three modes in order.
  In the Sorter, items waited for a valid filter. Shift-clicking a Name Tag
  renamed to `forge:ores` configured the filter and sent all three iron ores
  and both dirt blocks to their respective chests.
  The Furnace, Resonator, and Enchanter showed live energy and progress after
  shift-clicking their inputs in the muted client. Verified each result and
  shift-clicked a produced iron ingot back into player inventory.
  Shift-clicking seven emeralds in the Trash Can placed them in its disposal
  slots, and they disappeared on the next server tick.
  Shift-clicking a named, damaged pickaxe into the Creative Chest produced a
  single copy with the same name and damage. Taking the output kept the original.
  The Golden Bag kept both deposited stacks on reopening and prevented the
  open bag from being dropped. Shift-clicking newly named books into the Gold
  Chest kept them separate from the differently named books already stored.
- [ ] Walk crafting progression through all gameplay systems. Check unresolved
  item tags, recipes, assets, and world generation data at startup and reload.
  A client resource reload resolved the missing Kikoku, Amazing Opinium Core,
  Portable Globe, and rainbow particle textures after correcting their paths.
- [x] Restore Gradle 8.8 wrapper, align the jar and mod versions, and make
  modpack deployment explicit. Built 1.7.3 with Java 17. The explicit deploy
  copy matched the jar when pointed at a temporary destination.
- [x] Run the current GameTest suite on Forge 47.4.10 with and without the
  optional development mods. All 82 tests passed in both configurations.
  Rerun as coverage grows.
- [ ] Build the final jar from a clean checkout and load that packaged jar in
  both the client and dedicated server. Packaged generator, processor, and
  pump fixes have loaded in a Forge 47.4.10 dedicated server installation. A
  later packaged jar also loaded in a muted Forge 47.4.10 client and exercised
  the Ender Quarry's four-marker setup through normal clicks.
- [ ] Upgrade an isolated copy of a representative 1.20.1 save. Check registry
  IDs, inventory contents, FE buffers, fuel and machine state, then restart and
  check again. A copy of the save retained the sampled Gold Chest slots, TNT
  fuel and FE, redstone mode, and named item data after upgrading to the build
  with shared generator logic and restarting. In an earlier upgrade, the
  previously idle east receiver was powered after loading the save at `697949b`.
  The processor and pump build also preserved named Furnace and Resonator
  output, completed an active legacy Enchanter job, and moved all 1,234 mB of
  saved pump fluid into a drum. Those results persisted after restart.
  A later upgrade retained the same drum's water in its legacy tank data
  through another dedicated-server restart.
  The quarry save also retained the Golden Bag and both stored stacks,
  including the name of the gold ingots, on upgrade and restart.
