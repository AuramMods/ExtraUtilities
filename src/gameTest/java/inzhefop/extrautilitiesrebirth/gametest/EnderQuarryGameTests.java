package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.entity.EnderQuarryBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.util.List;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class EnderQuarryGameTests {
    private static final BlockPos QUARRY = new BlockPos(1, 2, 1);
    private static final BlockPos OUTPUT = QUARRY.east();
    private static final BlockPos FIRST_BLOCK = new BlockPos(4, 3, 4);

    @GameTest(template = "machine_test")
    public static void quarryPausesAndResumesWithoutOverwritingDrops(GameTestHelper test) {
        test.setBlock(OUTPUT, Blocks.CHEST);
        test.setBlock(FIRST_BLOCK, Blocks.DIAMOND_BLOCK);
        test.setBlock(FIRST_BLOCK.below(), Blocks.GOLD_BLOCK);
        EnderQuarryBlockEntity quarry = placeQuarry(test);
        ChestBlockEntity chest = (ChestBlockEntity) test.getBlockEntity(OUTPUT);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        ItemStack buffered = new ItemStack(Items.DIRT, 16);
        buffered.setHoverName(Component.literal("Buffered before mining"));
        quarry.setItem(0, buffered);
        startQuarry(test, quarry, true);

        test.runAfterDelay(4, () -> {
            test.assertBlockPresent(Blocks.DIAMOND_BLOCK, FIRST_BLOCK);
            test.assertBlockPresent(Blocks.GOLD_BLOCK, FIRST_BLOCK.below());
            test.assertTrue(quarry.getItem(0).is(Items.DIRT) && quarry.getItem(0).getCount() == 16,
                    "A full destination must leave the existing output intact");
            chest.clearContent();
        });
        test.runAfterDelay(10, () -> {
            test.assertBlockPresent(Blocks.AIR, FIRST_BLOCK);
            test.assertBlockPresent(Blocks.AIR, FIRST_BLOCK.below());
            test.assertTrue(ItemStack.matches(chest.getItem(0), buffered),
                    "The original output was not delivered: " + chest.getItem(0));
            test.assertTrue(chest.getItem(1).is(Items.DIAMOND_BLOCK), "The first mined drop was lost");
            test.assertTrue(chest.getItem(2).is(Items.GOLD_BLOCK), "The second mined drop was lost");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void quarryPreservesDestinationStackData(GameTestHelper test) {
        test.setBlock(OUTPUT, Blocks.CHEST);
        EnderQuarryBlockEntity quarry = placeQuarry(test);
        ChestBlockEntity chest = (ChestBlockEntity) test.getBlockEntity(OUTPUT);
        ItemStack original = new ItemStack(Items.DIAMOND, 63);
        original.setHoverName(Component.literal("Original"));
        chest.setItem(0, original);
        ItemStack incoming = new ItemStack(Items.DIAMOND);
        incoming.setHoverName(Component.literal("Incoming"));
        quarry.setItem(0, incoming);

        test.runAfterDelay(4, () -> {
            test.assertTrue(chest.getItem(0).getCount() == 63
                    && chest.getItem(0).getHoverName().getString().equals("Original"),
                    "An incompatible stack in the destination was modified");
            test.assertTrue(chest.getItem(1).getCount() == 1
                    && chest.getItem(1).getHoverName().getString().equals("Incoming"),
                    "The incoming item was not routed to a separate slot");
            test.assertTrue(quarry.getItem(0).isEmpty(), "The delivered item is still buffered");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void quarryHonorsSidedSlotsAndPartialTransfer(GameTestHelper test) {
        test.setBlock(OUTPUT, Blocks.FURNACE);
        EnderQuarryBlockEntity quarry = placeQuarry(test);
        FurnaceBlockEntity furnace = (FurnaceBlockEntity) test.getBlockEntity(OUTPUT);
        furnace.setItem(1, new ItemStack(Items.COAL, 63));
        quarry.setItem(0, new ItemStack(Items.COAL, 8));

        test.runAfterDelay(4, () -> {
            test.assertTrue(furnace.getItem(0).isEmpty(), "The side must not expose the smelting input");
            test.assertTrue(furnace.getItem(1).getCount() == 64, "The fuel slot should receive one coal");
            test.assertTrue(furnace.getItem(2).isEmpty(), "The side must not expose the output");
            test.assertTrue(quarry.getItem(0).getCount() == 7, "The rejected coal must remain buffered");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void quarryPreservesMultipleDropsThroughSaveAndLoad(GameTestHelper test) {
        test.setBlock(FIRST_BLOCK, Blocks.DECORATED_POT);
        CompoundTag pot = new CompoundTag();
        ListTag sherds = new ListTag();
        for (String item : List.of("angler_pottery_sherd", "archer_pottery_sherd", "arms_up_pottery_sherd", "brick")) {
            sherds.add(StringTag.valueOf("minecraft:" + item));
        }
        pot.put("sherds", sherds);
        test.getBlockEntity(FIRST_BLOCK).load(pot);

        EnderQuarryBlockEntity quarry = placeQuarry(test);
        startQuarry(test, quarry, false);
        test.runAfterDelay(4, () -> {
            test.assertBlockPresent(Blocks.AIR, FIRST_BLOCK);
            CompoundTag snapshot = quarry.saveWithFullMetadata();
            snapshot.getCompound("ForgeData").putDouble("state", 2);
            quarry.load(snapshot.copy());
            test.setBlock(OUTPUT, Blocks.CHEST);
        });
        test.runAfterDelay(8, () -> {
            ChestBlockEntity chest = (ChestBlockEntity) test.getBlockEntity(OUTPUT);
            for (var item : List.of(Items.ANGLER_POTTERY_SHERD, Items.ARCHER_POTTERY_SHERD,
                    Items.ARMS_UP_POTTERY_SHERD, Items.BRICK)) {
                test.assertTrue(chest.countItem(item) == 1, "Expected exactly one saved drop of " + item);
            }
            test.assertTrue(quarry.getItem(0).isEmpty(), "Saved drops should all have been exported");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void quarryHarvestsDirtAndReplacesItInNormalMode(GameTestHelper test) {
        test.setBlock(FIRST_BLOCK, Blocks.DIRT);
        EnderQuarryBlockEntity quarry = placeQuarry(test);
        startQuarry(test, quarry, false);
        quarry.getPersistentData().putBoolean("eqhole", false);

        test.runAfterDelay(4, () -> {
            test.assertBlockPresent(Blocks.DIRT, FIRST_BLOCK);
            test.assertTrue(quarry.getItem(0).is(Items.DIRT) && quarry.getItem(0).getCount() == 1,
                    "The quarry must harvest the original block before placing fill");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test", timeoutTicks = 180)
    public static void enderQuarryStartsFromFourLinkedMarkersAndUsesSilkTouch(GameTestHelper test) {
        BlockPos anchor = new BlockPos(5, 3, 5);
        BlockPos markerCorner = anchor.north(4).west(4);
        var marker = ExtrautilitiesrebirthModBlocks.ENDER_MARKER.get();
        test.setBlock(anchor, marker);
        test.setBlock(anchor.north(4), marker);
        test.setBlock(anchor.west(4), marker);
        test.useBlock(anchor);
        test.assertTrue(!test.getBlockEntity(anchor).getPersistentData().getBoolean("complete"),
                "Three markers must not establish a rectangular boundary");
        test.setBlock(markerCorner, marker);
        test.useBlock(anchor);
        test.assertTrue(test.getBlockEntity(anchor).getPersistentData().getBoolean("complete"),
                "Right-clicking the corner must link all four markers");

        BlockPos machine = anchor.east();
        BlockPos destination = machine.above();
        BlockPos firstOre = anchor.north().west().below();
        BlockPos outside = anchor.north().east().below();
        test.setBlock(firstOre, Blocks.DIAMOND_ORE);
        test.setBlock(outside, Blocks.GOLD_ORE);
        test.setBlock(machine, ExtrautilitiesrebirthModBlocks.ENDER_QUARRY.get());
        test.setBlock(destination, Blocks.CHEST);
        test.setBlock(machine.north(), ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE.get());
        test.setBlock(machine.south(), ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SPEED_UPGRADE.get());
        BlockEntity quarry = test.getBlockEntity(machine);
        test.useBlock(machine);
        test.assertTrue(quarry.getPersistentData().getInt("state") == 1,
                "The quarry must start when used next to the linked marker");
        test.assertTrue(quarry.getPersistentData().getInt("eqspeed") == 3
                        && quarry.getPersistentData().getBoolean("eqsilk"),
                "Placing the adjacent Speed and Silk Touch upgrades must affect the quarry");

        var energy = quarry.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
        test.assertTrue(energy.receiveEnergy(500_000, false) == 500_000, "The quarry must accept Forge Energy");
        var chest = (ChestBlockEntity) test.getBlockEntity(destination);
        test.succeedWhen(() -> {
            test.assertTrue(chest.countItem(Items.DIAMOND_ORE) == 1, "Silk Touch must deliver intact ore to the chest");
            test.assertBlockPresent(Blocks.DIRT, firstOre);
            test.assertBlockPresent(Blocks.GOLD_ORE, outside);
            test.assertTrue(energy.getEnergyStored() < 500_000, "Mining must use stored Forge Energy");
        });
    }

    @GameTest(template = "machine_test", timeoutTicks = 280)
    public static void enderQuarryTraversesTheWholeMarkerRectangleInWorldHoleMode(GameTestHelper test) {
        BlockPos anchor = new BlockPos(5, 3, 5);
        var marker = ExtrautilitiesrebirthModBlocks.ENDER_MARKER.get();
        for (BlockPos pos : List.of(anchor, anchor.north(3), anchor.west(3), anchor.north(3).west(3))) {
            test.setBlock(pos, marker);
        }
        test.useBlock(anchor);
        BlockPos machine = anchor.east();
        BlockPos destination = machine.above();
        List<BlockPos> topBlocks = List.of(anchor.north().west().below(), anchor.north(2).west().below(),
                anchor.north().west(2).below(), anchor.north(2).west(2).below());
        for (BlockPos block : topBlocks) {
            test.setBlock(block, Blocks.DIAMOND_BLOCK);
        }
        BlockPos outside = anchor.north().east().below();
        test.setBlock(outside, Blocks.GOLD_BLOCK);
        test.setBlock(machine, ExtrautilitiesrebirthModBlocks.ENDER_QUARRY.get());
        test.setBlock(destination, Blocks.CHEST);
        test.setBlock(machine.north(), ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_WORLD_HOLE_UPGRADE.get());
        test.setBlock(machine.south(), ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_3.get());
        test.useBlock(machine);
        var quarry = test.getBlockEntity(machine);
        test.assertTrue(quarry.getPersistentData().getBoolean("eqhole"), "The adjacent World Hole upgrade must activate");
        quarry.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new).receiveEnergy(1_000_000, false);

        var chest = (ChestBlockEntity) test.getBlockEntity(destination);
        test.succeedWhen(() -> {
            test.assertTrue(quarry.getPersistentData().getInt("state") == 2, "The quarry must finish all four columns");
            for (BlockPos block : topBlocks) {
                test.assertBlockPresent(Blocks.AIR, block);
            }
            test.assertTrue(chest.countItem(Items.DIAMOND_BLOCK) == 4, "All bounded drops must reach the chest");
            test.assertBlockPresent(Blocks.GOLD_BLOCK, outside);
        });
    }

    private static EnderQuarryBlockEntity placeQuarry(GameTestHelper test) {
        test.setBlock(QUARRY, ExtrautilitiesrebirthModBlocks.ENDER_QUARRY.get());
        return (EnderQuarryBlockEntity) test.getBlockEntity(QUARRY);
    }

    private static void startQuarry(GameTestHelper test, EnderQuarryBlockEntity quarry, boolean silk) {
        BlockPos target = test.absolutePos(FIRST_BLOCK);
        CompoundTag data = quarry.getPersistentData();
        data.putDouble("state", 1);
        data.putDouble("eqspeed", 16);
        data.putBoolean("eqsilk", silk);
        data.putDouble("eqfortune", 1);
        data.putBoolean("eqhole", true);
        data.putDouble("xcoord", target.getX());
        data.putDouble("ycoord", target.getY());
        data.putDouble("zcoord", target.getZ());
    }
}
