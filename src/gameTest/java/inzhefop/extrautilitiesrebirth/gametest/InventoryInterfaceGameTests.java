package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class InventoryInterfaceGameTests {
    @GameTest(template = "machine_test", timeoutTicks = 100)
    public static void inventoryInterfaceRestoresTenSlotsAndSuppliesAHopper(GameTestHelper test) {
        var pos = new BlockPos(3, 3, 3);
        var hopper = pos.below();
        var destination = hopper.south();
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.INVENTORY_INTERFACE.get());
        var entity = test.getBlockEntity(pos);
        var input = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).orElseThrow(AssertionError::new);
        var stored = new ItemStack(Items.IRON_INGOT, 3);
        stored.setHoverName(Component.literal("From the interface"));
        test.assertTrue(input.getSlots() == 10 && input.insertItem(9, stored, false).isEmpty(),
                "Automation should be able to use the last of the ten slots");
        entity.load(entity.saveWithFullMetadata());
        var output = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
        test.assertTrue(ItemStack.matches(stored, output.getStackInSlot(9)), "The saved stack should retain its name and count");

        var level = test.getLevel();
        var actual = test.absolutePos(pos);
        var state = level.getBlockState(actual);
        test.assertTrue(state.getAnalogOutputSignal(level, actual) > 0, "The interface should report its inventory to comparators");
        test.setBlock(destination, Blocks.CHEST);
        test.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));
        var chest = (ChestBlockEntity) test.getBlockEntity(destination);
        test.succeedWhen(() -> {
            test.assertTrue(ItemStack.matches(stored, chest.getItem(0)) && output.getStackInSlot(9).isEmpty(),
                    "A vanilla hopper should pull and deliver the whole named stack");
            test.assertTrue(state.getAnalogOutputSignal(level, actual) == 0, "The empty interface should clear its comparator");
        });
    }

    @GameTest(template = "machine_test")
    public static void inventoryInterfaceUsesItsDeclaredHarvestToolAndAutomationOnly(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        var level = test.getLevel();
        var actual = test.absolutePos(pos);
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.INVENTORY_INTERFACE.get());
        var state = level.getBlockState(actual);
        test.assertTrue(state.getMenuProvider(level, actual) == null, "The bundled automation buffer should have no menu");
        var player = test.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        test.assertTrue(!state.canHarvestBlock(level, actual, player), "An axe should not harvest the interface");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
        test.assertTrue(!state.canHarvestBlock(level, actual, player), "A stone pickaxe should be below the specified tier");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        test.assertTrue(state.canHarvestBlock(level, actual, player), "An iron pickaxe should harvest the interface");
        test.succeed();
    }
}
