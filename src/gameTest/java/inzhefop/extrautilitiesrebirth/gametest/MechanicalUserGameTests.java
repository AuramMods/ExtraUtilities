package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.MechanicalUserBlock;
import inzhefop.extrautilitiesrebirth.block.entity.MechanicalUserBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.world.inventory.MechanicalUserGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class MechanicalUserGameTests {
    private static final BlockPos USER = new BlockPos(3, 2, 3);
    private static final BlockPos TARGET = USER.east();

    @GameTest(template = "machine_test")
    public static void placesWithNormalOrientationAndPreservesBlockEntityData(GameTestHelper test) {
        var user = placeUser(test);
        test.setBlock(TARGET.below(), Blocks.STONE);
        user.setItem(0, new ItemStack(Items.OAK_SAPLING));
        test.runAfterDelay(29, () -> {
            test.assertBlockPresent(Blocks.AIR, TARGET);
            test.assertTrue(user.getItem(0).is(Items.OAK_SAPLING), "An invalid placement must retain its input");
            var stored = NonNullList.withSize(27, ItemStack.EMPTY);
            stored.set(26, new ItemStack(Items.EMERALD, 5));
            var shulker = new ItemStack(Items.SHULKER_BOX).setHoverName(Component.literal("Packed"));
            BlockItem.setBlockEntityData(shulker, BlockEntityType.SHULKER_BOX,
                    ContainerHelper.saveAllItems(new CompoundTag(), stored));
            user.setItem(0, shulker);
        });
        test.runAfterDelay(59, () -> {
            test.assertBlockPresent(Blocks.SHULKER_BOX, TARGET);
            var entity = (ShulkerBoxBlockEntity) test.getBlockEntity(TARGET);
            test.assertTrue(entity.getBlockState().getValue(BlockStateProperties.FACING) == Direction.EAST,
                    "The shulker should face outward from the Mechanical User");
            test.assertTrue(entity.getItem(26).is(Items.EMERALD) && entity.getItem(26).getCount() == 5
                            && entity.getDisplayName().getString().equals("Packed") && user.getItem(0).isEmpty(),
                    "A successful placement should copy the name and contents, then use one item");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void menuSelectsDropModeAndKeepsItemTagsOnReload(GameTestHelper test) {
        var user = placeUser(test);
        var player = test.makeMockPlayer();
        player.setPos(Vec3.atCenterOf(test.absolutePos(USER)));
        var menu = (MechanicalUserGUIMenu) user.createMenu(1, player.getInventory());
        test.setBlock(TARGET, Blocks.STONE);
        test.assertTrue(menu.clickMenuButton(player, 0) && user.getPersistentData().getInt("machinemode") == 2,
                "The opened menu should cycle from placement to item drop");
        user.setItem(0, new ItemStack(Items.DIAMOND, 2).setHoverName(Component.literal("Delivery")));
        user.load(user.saveWithFullMetadata());

        test.runAfterDelay(5, () -> {
            test.assertTrue(energyOf(user).getEnergyStored() == 25_000 && user.getItem(0).getCount() == 2,
                    "The user should wait while the output is obstructed");
            test.setBlock(TARGET, Blocks.AIR);
        });
        test.runAfterDelay(35, () -> {
            var drops = test.getEntities(EntityType.ITEM, TARGET, 1.5);
            test.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.DIAMOND)
                    && drop.getItem().getCount() == 1 && drop.getItem().getHoverName().getString().equals("Delivery")),
                    "The item dropped after reload should retain its custom data");
            test.assertTrue(user.getItem(0).getCount() == 1, "A completed drop should consume exactly one item");
            test.setBlock(USER, Blocks.AIR);
            test.assertTrue(!menu.stillValid(player), "A removed machine should close its menu");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void attackModeWaitsForMobsAndUsesSwordDurability(GameTestHelper test) {
        var user = placeUser(test);
        var player = test.makeMockPlayer();
        player.setPos(Vec3.atCenterOf(test.absolutePos(USER)));
        var menu = (MechanicalUserGUIMenu) user.createMenu(1, player.getInventory());
        test.assertTrue(menu.clickMenuButton(player, 0) && menu.clickMenuButton(player, 0)
                        && user.mode() == MechanicalUserBlockEntity.Mode.ATTACK,
                "Two button presses should select attack mode");
        var input = user.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).orElseThrow(AssertionError::new);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        test.assertTrue(!input.insertItem(8, sword, false).isEmpty()
                        && !menu.getSlot(8).mayPlace(sword) && input.insertItem(0, sword, false).isEmpty(),
                "The input accepts a sword while the upgrade slot rejects it");
        test.assertTrue(input.extractItem(0, 1, false).isEmpty(), "Sided extraction should leave the sword in place");
        test.runAfterDelay(10, () -> {
            test.assertTrue(energyOf(user).getEnergyStored() == 25_000, "Attack mode should wait for a mob");
            var zombie = test.spawn(EntityType.ZOMBIE, TARGET);
            zombie.setNoAi(true);
            test.runAfterDelay(30, () -> {
                test.assertTrue(zombie.getHealth() == 16 && user.getItem(0).getDamageValue() == 1,
                        "A completed attack should damage the mob and wear the sword once");
                test.succeed();
            });
        });
    }

    private static MechanicalUserBlockEntity placeUser(GameTestHelper test) {
        test.setBlock(USER, ExtrautilitiesrebirthModBlocks.MECHANICAL_USER.get().defaultBlockState()
                .setValue(MechanicalUserBlock.FACING, Direction.EAST));
        var user = (MechanicalUserBlockEntity) test.getBlockEntity(USER);
        user.setItem(8, new ItemStack(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get(), 8));
        energyOf(user).receiveEnergy(25_000, false);
        return user;
    }

    private static IEnergyStorage energyOf(MechanicalUserBlockEntity user) {
        return user.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
    }
}
