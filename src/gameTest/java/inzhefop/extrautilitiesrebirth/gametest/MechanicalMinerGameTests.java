package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.MechanicalMinerBlock;
import inzhefop.extrautilitiesrebirth.block.entity.MechanicalMinerBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.world.inventory.MechanicalMinerGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class MechanicalMinerGameTests {
    private static final BlockPos MINER = new BlockPos(3, 2, 3);
    private static final BlockPos TARGET = MINER.east();

    @GameTest(template = "machine_test")
    public static void minesOnlyTargetDropsAndKeepsNamedStacksSeparate(GameTestHelper test) {
        var miner = placeMiner(test);
        miner.setItem(0, new ItemStack(Items.DIAMOND).setHoverName(Component.literal("Kept")));
        test.setBlock(TARGET, Blocks.DIAMOND_ORE);
        var nearbyItem = test.spawnItem(Items.DIAMOND, TARGET.south());
        nearbyItem.getItem().setHoverName(Component.literal("Unrelated"));
        nearbyItem.setNoGravity(true);

        test.runAfterDelay(30, () -> {
            test.assertBlockPresent(Blocks.AIR, TARGET);
            test.assertTrue(nearbyItem.isAlive() && nearbyItem.getItem().getHoverName().getString().equals("Unrelated"),
                    "Mining should leave unrelated ground items alone");
            test.assertTrue(miner.getItem(0).getCount() == 1 && miner.getItem(0).getHoverName().getString().equals("Kept"),
                    "The miner must retain the named item already in storage");
            test.assertTrue(miner.getItem(1).is(Items.DIAMOND) && !miner.getItem(1).hasCustomHoverName(),
                    "The ore should be collected in a separate stack");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void buffersMultipleDropsAndResumesAfterReload(GameTestHelper test) {
        var miner = placeMiner(test);
        for (int slot = 0; slot < 9; slot++) {
            miner.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        test.setBlock(TARGET.below(), Blocks.END_STONE);
        test.setBlock(TARGET, ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_8.get());

        test.runAfterDelay(30, () -> {
            test.assertBlockPresent(ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_8.get(), TARGET);
            test.assertTrue(energyOf(miner).getEnergyStored() == 25_000, "A full output should pause mining and preserve energy");
            miner.setItem(8, ItemStack.EMPTY);
        });
        test.runAfterDelay(60, () -> {
            test.assertBlockPresent(Blocks.AIR, TARGET);
            test.assertTrue(miner.getItem(8).is(ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_1.get().asItem()),
                    "The seed should occupy the freed output slot");
            miner.load(miner.saveWithFullMetadata());
            test.setBlock(TARGET, Blocks.DIAMOND_ORE);
        });
        test.runAfterDelay(65, () -> {
            test.assertBlockPresent(Blocks.DIAMOND_ORE, TARGET);
            var output = miner.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
            test.assertTrue(output.extractItem(8, 1, false).is(ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_1.get().asItem()),
                    "Sided automation should extract the seed from its output slot");
        });
        test.runAfterDelay(70, () -> {
            test.assertTrue(miner.getItem(8).is(Items.ENDER_PEARL) && miner.getItem(8).getCount() == 1,
                    "The pending pearl must remain available after loading the saved miner");
            test.assertBlockPresent(Blocks.DIAMOND_ORE, TARGET);
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void menuRoutesUpgradesAndClosesOnReplacement(GameTestHelper test) {
        test.setBlock(MINER, ExtrautilitiesrebirthModBlocks.MECHANICAL_MINER.get());
        var miner = (MechanicalMinerBlockEntity) test.getBlockEntity(MINER);
        var player = test.makeMockPlayer();
        player.setPos(Vec3.atCenterOf(test.absolutePos(MINER)));
        var inventory = player.getInventory();
        inventory.setItem(0, new ItemStack(Items.DIAMOND));
        inventory.setItem(1, speedUpgrade());
        var menu = (MechanicalMinerGUIMenu) miner.createMenu(1, inventory);
        int hotbarStart = menu.slots.size() - 9;

        test.assertTrue(menu.stillValid(player), "The menu should be valid in reach of the miner");
        test.assertTrue(!menu.getSlot(0).mayPlace(speedUpgrade()) && !menu.getSlot(9).mayPlace(inventory.getItem(0)),
                "The outputs should reject input and the upgrade slot should reject unrelated items");
        test.assertTrue(menu.quickMoveStack(player, hotbarStart).is(Items.DIAMOND) && inventory.getItem(9).is(Items.DIAMOND),
                "Shift-clicking an unrelated item should move it within the player's inventory");
        test.assertTrue(menu.quickMoveStack(player, hotbarStart + 1).getCount() == 10 && miner.getItem(9).getCount() == 10,
                "Shift-clicking upgrades should fill the miner's upgrade slot");
        test.setBlock(MINER, Blocks.AIR);
        test.assertTrue(!menu.stillValid(player), "Removing the miner should invalidate the menu");
        test.succeed();
    }

    private static MechanicalMinerBlockEntity placeMiner(GameTestHelper test) {
        test.setBlock(MINER, ExtrautilitiesrebirthModBlocks.MECHANICAL_MINER.get().defaultBlockState()
                .setValue(MechanicalMinerBlock.FACING, Direction.EAST));
        var miner = (MechanicalMinerBlockEntity) test.getBlockEntity(MINER);
        miner.setItem(9, speedUpgrade());
        energyOf(miner).receiveEnergy(25_000, false);
        return miner;
    }

    private static ItemStack speedUpgrade() {
        return new ItemStack(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get(), 10);
    }

    private static IEnergyStorage energyOf(MechanicalMinerBlockEntity miner) {
        return miner.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
    }
}
