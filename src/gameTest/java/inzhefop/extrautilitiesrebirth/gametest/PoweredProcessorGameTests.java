package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.entity.EnchanterBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.FurnaceBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.ResonatorBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class PoweredProcessorGameTests {
    private static final BlockPos MACHINE = new BlockPos(3, 2, 3);

    @GameTest(template = "machine_test", timeoutTicks = 230)
    public static void furnaceKeepsTheRecipeOutputCountAndData(GameTestHelper test) {
        test.setBlock(MACHINE, ExtrautilitiesrebirthModBlocks.FURNACE.get());
        var furnace = (FurnaceBlockEntity) test.getBlockEntity(MACHINE);
        furnace.setItem(0, new ItemStack(Items.AMETHYST_CLUSTER));
        furnace.setItem(1, new ItemStack(Items.DIAMOND, 63).setHoverName(Component.literal("Smelted Alloy")));
        var energy = energyOf(furnace);
        energy.receiveEnergy(2_000, false);
        test.runAfterDelay(105, () -> {
            test.assertTrue(furnace.getItem(0).getCount() == 1 && furnace.getItem(1).getCount() == 63
                    && energy.getEnergyStored() == 2_000, "A batch that cannot fit must keep its input, output, and FE");
            furnace.setItem(1, ItemStack.EMPTY);
        });
        test.runAfterDelay(210, () -> {
            ItemStack result = furnace.getItem(1);
            test.assertTrue(furnace.getItem(0).isEmpty() && result.is(Items.DIAMOND) && result.getCount() == 3
                            && result.getHoverName().getString().equals("Smelted Alloy") && energy.getEnergyStored() == 1_000,
                    "One recipe should yield its three named items for the established 1,000 FE charge");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test", timeoutTicks = 135)
    public static void resonatorUsesExactEnergyAfterLoadingAnActiveJob(GameTestHelper test) {
        test.setBlock(MACHINE, ExtrautilitiesrebirthModBlocks.RESONATOR.get());
        var resonator = (ResonatorBlockEntity) test.getBlockEntity(MACHINE);
        var energy = energyOf(resonator);
        energy.receiveEnergy(8_000, false);
        var input = resonator.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        test.assertTrue(input.insertItem(0, new ItemStack(Items.SMOOTH_QUARTZ), false).isEmpty(), "Automation should accept quartz");
        test.runAfterDelay(40, () -> {
            var saved = resonator.saveWithFullMetadata();
            saved.getCompound("ForgeData").remove("ProcessingRecipe");
            test.assertTrue(saved.getCompound("ForgeData").getDouble("ticks") > 0,
                    "The legacy saved job should contain partial progress");
            resonator.load(saved);
        });
        test.runAfterDelay(110, () -> {
            var output = resonator.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
            test.assertTrue(output.extractItem(1, 1, false).is(ExtrautilitiesrebirthModBlocks.QUARTZBURNT.get().asItem())
                            && energy.getEnergyStored() == 0, "At exact power the resumed job should finish and expose its output");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void resonatorProcessesEveryLegacyRecipe(GameTestHelper test) {
        var recipes = List.of(
                new ResonatorCase(ExtrautilitiesrebirthModBlocks.POLISHED_STONE.get().asItem(),
                        ExtrautilitiesrebirthModBlocks.STONEBURNT.get().asItem(), 8_000),
                new ResonatorCase(Items.SMOOTH_QUARTZ, ExtrautilitiesrebirthModBlocks.QUARTZBURNT.get().asItem(), 8_000),
                new ResonatorCase(ExtrautilitiesrebirthModBlocks.STONEBURNT.get().asItem(),
                        ExtrautilitiesrebirthModBlocks.RAINBOW_STONE.get().asItem(), 64_000),
                new ResonatorCase(Items.LAPIS_LAZULI, ExtrautilitiesrebirthModItems.LUNAR_REACTIVE_DUST.get(), 16_000),
                new ResonatorCase(Items.COAL, ExtrautilitiesrebirthModItems.RED_COAL.get(), 16_000),
                new ResonatorCase(Items.LIGHT_WEIGHTED_PRESSURE_PLATE, ExtrautilitiesrebirthModItems.UPGRADE_BASE.get(), 8_000),
                new ResonatorCase(Items.IRON_BARS, ExtrautilitiesrebirthModItems.WIRELESS_RF_HEATING_COIL.get(), 16_000));

        for (int index = 0; index < recipes.size(); index++) {
            BlockPos pos = rowPosition(index);
            var recipe = recipes.get(index);
            test.setBlock(pos, ExtrautilitiesrebirthModBlocks.RESONATOR.get());
            var resonator = (ResonatorBlockEntity) test.getBlockEntity(pos);
            var input = resonator.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
            test.assertTrue(input.insertItem(0, new ItemStack(recipe.input()), false).isEmpty(),
                    "Automation should accept " + recipe.input());
            resonator.setItem(2, new ItemStack(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get(), 6));
            energyOf(resonator).receiveEnergy(recipe.energy(), false);
            test.runAfterDelay(12, () -> test.assertTrue(resonator.getItem(0).isEmpty()
                    && resonator.getItem(1).is(recipe.output()) && resonator.getItem(1).getCount() == 1
                    && energyOf(resonator).getEnergyStored() == 0, "The resonator should produce " + recipe.output()));
        }
        test.runAfterDelay(13, test::succeed);
    }

    @GameTest(template = "machine_test", timeoutTicks = 240)
    public static void enchanterDoesNotOverwriteNamedOutput(GameTestHelper test) {
        test.setBlock(MACHINE, ExtrautilitiesrebirthModBlocks.ENCHANTER.get());
        var enchanter = (EnchanterBlockEntity) test.getBlockEntity(MACHINE);
        var original = new ItemStack(ExtrautilitiesrebirthModItems.ENCHANTED_INGOT.get()).setHoverName(Component.literal("Keep me"));
        enchanter.setItem(0, new ItemStack(Items.GOLD_INGOT));
        enchanter.setItem(1, new ItemStack(Items.LAPIS_LAZULI));
        enchanter.setItem(2, original.copy());
        var energy = energyOf(enchanter);
        energy.receiveEnergy(58_000, false);
        enchanter.setItem(3, new ItemStack(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get(), 30));
        test.runAfterDelay(20, () -> {
            test.assertTrue(ItemStack.matches(enchanter.getItem(2), original) && enchanter.getItem(0).is(Items.GOLD_INGOT)
                    && energy.getEnergyStored() == 58_000, "A named output stack must block incompatible output");
            enchanter.setItem(2, ItemStack.EMPTY);
        });
        test.runAfterDelay(40, () -> {
            test.assertTrue(enchanter.getItem(0).isEmpty() && enchanter.getItem(1).isEmpty()
                    && enchanter.getItem(2).is(ExtrautilitiesrebirthModItems.ENCHANTED_INGOT.get())
                    && !enchanter.getItem(2).hasCustomHoverName() && energy.getEnergyStored() == 38_000,
                    "Removing the named item should let the legacy recipe finish without losing its inputs");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test", timeoutTicks = 120)
    public static void enchanterMakesTheQuarryUpgradeWithAnEfficiencyBook(GameTestHelper test) {
        test.setBlock(MACHINE, ExtrautilitiesrebirthModBlocks.ENCHANTER.get());
        var enchanter = (EnchanterBlockEntity) test.getBlockEntity(MACHINE);
        var energy = energyOf(enchanter);
        energy.receiveEnergy(250_000, false);
        enchanter.setItem(0, new ItemStack(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SPEED_UPGRADE.get()));
        enchanter.setItem(1, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 3)));
        enchanter.setItem(3, new ItemStack(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get(), 64));
        test.runAfterDelay(50, () -> {
            test.assertTrue(enchanter.getItem(0).isEmpty() && enchanter.getItem(1).isEmpty()
                    && enchanter.getItem(2).is(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_2.get().asItem()),
                    "A real Efficiency book and legal speed stack should produce the tier-two quarry upgrade");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test", timeoutTicks = 270)
    public static void enchanterKeepsLegacyIngredientsAndBatchSizes(GameTestHelper test) {
        var recipes = List.of(
                new EnchanterCase(new ItemStack(Items.BOOKSHELF), new ItemStack(Items.LAPIS_LAZULI),
                        new ItemStack(ExtrautilitiesrebirthModBlocks.MAGICAL_WOOD.get()), 64_000),
                new EnchanterCase(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.LAPIS_LAZULI),
                        new ItemStack(ExtrautilitiesrebirthModItems.ENCHANTED_INGOT.get()), 8_000),
                new EnchanterCase(new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.LAPIS_LAZULI, 9),
                        new ItemStack(ExtrautilitiesrebirthModBlocks.BLOCK_OF_ENCHANED_METAL.get()), 24_000),
                new EnchanterCase(new ItemStack(Items.IRON_BLOCK, 8), new ItemStack(Items.NETHER_STAR, 9),
                        new ItemStack(ExtrautilitiesrebirthModBlocks.BLOCK_OF_EVIL_INFUSED_IRON.get(), 8), 192_000),
                new EnchanterCase(new ItemStack(Items.APPLE, 16), new ItemStack(Items.LAPIS_LAZULI, 4),
                        new ItemStack(ExtrautilitiesrebirthModItems.MAGICAL_APPLE.get(), 16), 16_000),
                new EnchanterCase(new ItemStack(Items.IRON_INGOT, 8), new ItemStack(Items.NETHER_STAR),
                        new ItemStack(ExtrautilitiesrebirthModItems.EVIL_INFUSED_IRON_INGOT.get(), 8), 24_000),
                new EnchanterCase(new ItemStack(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_UPGRADE_BASE.get()),
                        EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SILK_TOUCH, 1)),
                        new ItemStack(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE.get()), 64_000),
                new EnchanterCase(new ItemStack(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SPEED_UPGRADE.get()),
                        EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 3)),
                        new ItemStack(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_2.get()), 64_000),
                new EnchanterCase(new ItemStack(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_2.get()),
                        EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 5)),
                        new ItemStack(ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_3.get()), 64_000));

        for (int index = 0; index < recipes.size(); index++) {
            var recipe = recipes.get(index);
            BlockPos pos = rowPosition(index);
            test.setBlock(pos, ExtrautilitiesrebirthModBlocks.ENCHANTER.get());
            var enchanter = (EnchanterBlockEntity) test.getBlockEntity(pos);
            var input = enchanter.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
            test.assertTrue(input.insertItem(0, recipe.input().copy(), false).isEmpty()
                            && input.insertItem(1, recipe.reagent().copy(), false).isEmpty(),
                    "Automation should accept the ingredients for " + recipe.output().getHoverName().getString());
            enchanter.setItem(3, new ItemStack(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get(), 12));
            energyOf(enchanter).receiveEnergy(recipe.energy(), false);
            test.runAfterDelay(255, () -> test.assertTrue(enchanter.getItem(0).isEmpty() && enchanter.getItem(1).isEmpty()
                    && ItemStack.matches(enchanter.getItem(2), recipe.output()) && energyOf(enchanter).getEnergyStored() == 0,
                    "The enchanter should make " + recipe.output() + " at the expected charge"));
        }
        test.runAfterDelay(256, test::succeed);
    }

    @GameTest(template = "machine_test")
    public static void furnaceMenuRoutesInputsAndClosesWhenRemoved(GameTestHelper test) {
        test.setBlock(MACHINE, ExtrautilitiesrebirthModBlocks.FURNACE.get());
        var furnace = (FurnaceBlockEntity) test.getBlockEntity(MACHINE);
        var player = test.makeMockPlayer();
        player.setPos(Vec3.atCenterOf(test.absolutePos(MACHINE)));
        var inventory = player.getInventory();
        inventory.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        var upgrade = new ItemStack(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get(), 2);
        inventory.setItem(1, upgrade.copy());
        inventory.setItem(2, new ItemStack(Items.NETHER_STAR));
        var menu = furnace.createMenu(1, inventory);

        test.assertTrue(menu.stillValid(player) && !menu.getSlot(1).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "The bound menu should expose an extract-only output");
        int hotbarStart = menu.slots.size() - 9;
        test.assertTrue(menu.quickMoveStack(player, hotbarStart).getCount() == 4 && furnace.getItem(0).is(Items.RAW_IRON),
                "The recipe input should be routed to the smelting slot");
        test.assertTrue(ItemStack.matches(menu.quickMoveStack(player, hotbarStart + 1), upgrade)
                        && ItemStack.matches(furnace.getItem(2), upgrade),
                "The upgrade should be routed to the upgrade slot");
        test.assertTrue(menu.quickMoveStack(player, hotbarStart + 2).is(Items.NETHER_STAR)
                        && inventory.getItem(9).is(Items.NETHER_STAR),
                "Unrelated items should stay in the player's inventory");
        test.setBlock(MACHINE, Blocks.AIR);
        test.assertTrue(!menu.stillValid(player), "Removing the furnace should close its menu");
        test.succeed();
    }

    private static IEnergyStorage energyOf(BlockEntity machine) {
        return machine.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
    }

    private static BlockPos rowPosition(int index) {
        return new BlockPos(1 + index % 3 * 2, 2, 1 + index / 3 * 2);
    }

    private record ResonatorCase(Item input, Item output, int energy) {
    }

    private record EnchanterCase(ItemStack input, ItemStack reagent, ItemStack output, int energy) {
    }
}
