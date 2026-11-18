package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.SortingMachineBlock;
import inzhefop.extrautilitiesrebirth.block.entity.SortingMachineBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.world.inventory.SorterGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class SortingMachineGameTests {
    private static final BlockPos SORTER = new BlockPos(3, 2, 3);
    private static final BlockPos MATCH = SORTER.west();
    private static final BlockPos OTHER = SORTER.east();

    @GameTest(template = "machine_test")
    public static void sortsBothWaysAndKeepsDifferentItemDataSeparate(GameTestHelper test) {
        var sorter = placeSorter(test);
        test.setBlock(MATCH, Blocks.CHEST);
        test.setBlock(OTHER, Blocks.CHEST);
        Container match = container(test, MATCH);
        Container other = container(test, OTHER);
        match.setItem(0, namedOre("Existing"));
        match.setItem(26, new ItemStack(Items.DIAMOND));
        sorter.setItem(0, namedOre("Sorted"));
        sorter.setItem(1, new ItemStack(Items.DIRT));

        test.runAfterDelay(6, () -> {
            test.assertTrue(match.getItem(0).getCount() == 1 && match.getItem(0).getHoverName().getString().equals("Existing"),
                    "The original ore stack must keep its name and count");
            test.assertTrue(match.getItem(1).is(Items.IRON_ORE) && match.getItem(1).getHoverName().getString().equals("Sorted")
                            && sorter.getItem(0).isEmpty(),
                    "Sorting should use open slots even when the last slot is occupied");
            test.assertTrue(other.getItem(0).is(Items.DIRT) && sorter.getItem(1).isEmpty(),
                    "The nonmatching item belongs on the opposite side");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void keepsRejectedItemsAndContinuesTheOtherRoute(GameTestHelper test) {
        var sorter = placeSorter(test);
        test.setBlock(MATCH, Blocks.FURNACE);
        test.setBlock(OTHER, Blocks.CHEST);
        sorter.setItem(0, namedOre("Pending"));
        sorter.setItem(1, new ItemStack(Items.DIRT));
        test.runAfterDelay(5, () -> {
            test.assertTrue(sorter.getItem(0).getHoverName().getString().equals("Pending") && container(test, MATCH).isEmpty(),
                    "A furnace's side should reject ore and leave it in the sorter");
            test.assertTrue(container(test, OTHER).getItem(0).is(Items.DIRT), "The other route should keep working");
            sorter.load(sorter.saveWithFullMetadata());
            test.setBlock(MATCH, Blocks.CHEST);
        });
        test.runAfterDelay(10, () -> {
            test.assertTrue(container(test, MATCH).getItem(0).getHoverName().getString().equals("Pending"),
                    "The retained item should route when a valid inventory replaces the furnace");
            test.assertTrue(sorter.getItem(0).isEmpty(), "A successful retry should consume the source stack");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void waitsForAValidFilterAndKeepsMenuSlotsSeparate(GameTestHelper test) {
        var sorter = placeSorter(test);
        test.setBlock(MATCH, Blocks.CHEST);
        test.setBlock(OTHER, Blocks.CHEST);
        sorter.setItem(16, new ItemStack(Items.NAME_TAG).setHoverName(Component.literal("forge:missing_sorter_tag")));
        sorter.setItem(0, namedOre("Waiting"));
        var player = test.makeMockPlayer();
        player.setPos(Vec3.atCenterOf(test.absolutePos(SORTER)));
        var menu = (SorterGUIMenu) sorter.createMenu(1, player.getInventory());
        test.assertTrue(!menu.getSlot(15).mayPlace(namedOre("Unwanted")) && !menu.getSlot(16).mayPlace(namedOre("Unwanted")),
                "Regular items should be rejected by the speed and filter slots");

        test.runAfterDelay(6, () -> {
            test.assertTrue(sorter.getItem(0).getCount() == 1 && container(test, MATCH).isEmpty() && container(test, OTHER).isEmpty(),
                    "An unknown filter must not send saved items down the wrong route");
            test.setBlock(SORTER, Blocks.AIR);
            test.assertTrue(!menu.stillValid(player), "A removed sorter should close its menu");
            test.succeed();
        });
    }

    private static SortingMachineBlockEntity placeSorter(GameTestHelper test) {
        test.setBlock(SORTER, ExtrautilitiesrebirthModBlocks.SORTING_MACHINE.get().defaultBlockState()
                .setValue(SortingMachineBlock.FACING, Direction.NORTH));
        var sorter = (SortingMachineBlockEntity) test.getBlockEntity(SORTER);
        sorter.setItem(15, new ItemStack(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get(), 19));
        sorter.setItem(16, new ItemStack(Items.NAME_TAG).setHoverName(Component.literal("forge:ores")));
        return sorter;
    }

    private static ItemStack namedOre(String name) {
        return new ItemStack(Items.IRON_ORE).setHoverName(Component.literal(name));
    }

    private static Container container(GameTestHelper test, BlockPos pos) {
        return (Container) test.getBlockEntity(pos);
    }
}
