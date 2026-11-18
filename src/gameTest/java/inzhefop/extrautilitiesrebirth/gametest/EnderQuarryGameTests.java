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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.util.List;
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
