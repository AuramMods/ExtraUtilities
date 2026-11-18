package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class CreativeChestGameTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    @GameTest(template = "machine_test")
    public static void creativeChestCopiesItemDataAndRespectsStackLimits(GameTestHelper test) {
        test.setBlock(POS, ExtrautilitiesrebirthModBlocks.CREATIVE_CHEST.get());
        var entity = test.getBlockEntity(POS);
        var player = test.makeMockPlayer();
        var center = Vec3.atCenterOf(test.absolutePos(POS));
        player.setPos(center.x, center.y, center.z);
        var menu = ((MenuProvider) entity).createMenu(1, player.getInventory(), player);
        var pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.setDamageValue(23);
        pickaxe.setHoverName(Component.literal("Template"));
        player.getInventory().setItem(9, pickaxe.copy());
        test.assertTrue(menu.quickMoveStack(player, 2).getCount() == 1 && player.getInventory().getItem(9).isEmpty(),
                "Shift-click should put the pickaxe into the template slot");
        test.assertTrue(!menu.getSlot(1).mayPlace(pickaxe), "The output should reject player insertion");

        test.runAfterDelay(3, () -> {
            test.assertTrue(ItemStack.matches(pickaxe, menu.getSlot(1).getItem()),
                    "The generated tool should be a single copy with the template's name and damage");
            test.assertTrue(ItemStack.matches(pickaxe, menu.quickMoveStack(player, 1)),
                    "Shift-click should retrieve the generated tool");
            test.assertTrue(ItemStack.matches(pickaxe, menu.getSlot(0).getItem()),
                    "Retrieving the copy should keep the original");
            entity.load(entity.saveWithFullMetadata());
        });
        test.runAfterDelay(6, () -> {
            test.assertTrue(ItemStack.matches(pickaxe, menu.getSlot(1).getItem()),
                    "A legacy save layout should retain the template and restore its output");
            test.setBlock(POS, Blocks.AIR);
            test.assertTrue(!menu.stillValid(player), "The menu should close when the chest is removed");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void creativeChestAutomationProtectsTemplateAndRefillsOutput(GameTestHelper test) {
        test.setBlock(POS, ExtrautilitiesrebirthModBlocks.CREATIVE_CHEST.get());
        var entity = test.getBlockEntity(POS);
        var input = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        var output = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
        var item = new ItemStack(Items.ENDER_PEARL);
        test.assertTrue(input.insertItem(1, item, false).getCount() == 1, "Automation should only insert into the sample slot");
        test.assertTrue(input.insertItem(0, item, false).isEmpty(), "Automation should be able to provide a sample");
        test.assertTrue(input.extractItem(0, 1, false).isEmpty() && output.extractItem(0, 1, false).isEmpty(),
                "Sided and unsided extraction should both protect the sample");

        test.runAfterDelay(3, () -> test.assertTrue(output.extractItem(1, 64, false).getCount() == 16,
                "Automation should pull a legal stack of sixteen pearls"));
        test.runAfterDelay(6, () -> {
            test.assertTrue(output.extractItem(1, 64, false).getCount() == 16,
                    "The chest should replenish its output");
            test.succeed();
        });
    }
}
