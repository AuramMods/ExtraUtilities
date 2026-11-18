package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class RainbowGeneratorGameTests {
    @GameTest(template = "machine_test")
    public static void rainbowGeneratorCraftsFromItsTwoHalves(GameTestHelper test) {
        ItemStack top = craft(test, ExtrautilitiesrebirthModBlocks.FURNACE_GENERATOR.get(),
                ExtrautilitiesrebirthModBlocks.DEATH_GENERATOR.get(), ExtrautilitiesrebirthModBlocks.HALITOSIS_GENERATOR.get(),
                ExtrautilitiesrebirthModBlocks.DISENCHANTMENT_GENERATOR.get(), ExtrautilitiesrebirthModItems.REDSTONE_CRYSTAL.get(),
                ExtrautilitiesrebirthModBlocks.ENDER_GENERATOR.get(), ExtrautilitiesrebirthModBlocks.FURNACE_GENERATOR.get(),
                ExtrautilitiesrebirthModBlocks.FROSTY_GENERATOR_1.get(), ExtrautilitiesrebirthModBlocks.MAGMA_GENERATOR.get());
        ItemStack bottom = craft(test, ExtrautilitiesrebirthModBlocks.NETHERSTAR_GENERATOR.get(),
                ExtrautilitiesrebirthModBlocks.OVERCLOCKED_GENERATOR.get(), ExtrautilitiesrebirthModBlocks.PINK_GENERATOR.get(),
                ExtrautilitiesrebirthModBlocks.POTION_GENERATOR_1.get(), ExtrautilitiesrebirthModItems.REDSTONE_CRYSTAL.get(),
                ExtrautilitiesrebirthModBlocks.REDSTONE_GENERATOR.get(), ExtrautilitiesrebirthModBlocks.SLIMEY_GENERATOR.get(),
                ExtrautilitiesrebirthModBlocks.SURVIVAL_GENERATOR.get(), ExtrautilitiesrebirthModBlocks.TNT_GENERATOR.get());
        test.assertTrue(top.is(ExtrautilitiesrebirthModBlocks.RAINMBOW_GENERATOR_TOP.get().asItem())
                        && bottom.is(ExtrautilitiesrebirthModBlocks.RAINBOW_GENERATOR_BOTTOM.get().asItem()),
                "The top and bottom recipes should produce the two Rainbow Generator parts");
        test.assertTrue(craft(test, top.getItem(), bottom.getItem()).is(ExtrautilitiesrebirthModBlocks.RAINBOW_GENERATOR.get().asItem()),
                "Crafting the two halves together should give the Rainbow Generator");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void rainbowGeneratorRunsWithSixteenWrenchLinkedFuels(GameTestHelper test) {
        BlockPos center = new BlockPos(3, 2, 3);
        test.setBlock(center, ExtrautilitiesrebirthModBlocks.RAINBOW_GENERATOR.get());
        var core = test.getBlockEntity(center);
        var energy = core.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);

        var player = test.makeMockPlayer();
        var wrench = new ItemStack(ExtrautilitiesrebirthModItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        player.setShiftKeyDown(true);
        test.assertTrue(useWrench(test, player, wrench, center).consumesAction(),
                "Sneak-use should select the Rainbow Generator");
        player.setShiftKeyDown(false);

        List<Fuel> fuels = List.of(
                new Fuel(ExtrautilitiesrebirthModBlocks.CULINARY_GENERATOR.get(), Items.BAKED_POTATO),
                new Fuel(ExtrautilitiesrebirthModBlocks.DEATH_GENERATOR.get(), Items.BONE),
                new Fuel(ExtrautilitiesrebirthModBlocks.DISENCHANTMENT_GENERATOR.get(), Items.ENCHANTED_BOOK),
                new Fuel(ExtrautilitiesrebirthModBlocks.ENDER_GENERATOR.get(), Items.ENDER_PEARL),
                new Fuel(ExtrautilitiesrebirthModBlocks.TNT_GENERATOR.get(), Items.TNT),
                new Fuel(ExtrautilitiesrebirthModBlocks.FROSTY_GENERATOR_1.get(), Items.SNOWBALL),
                new Fuel(ExtrautilitiesrebirthModBlocks.FURNACE_GENERATOR.get(), Items.COAL),
                new Fuel(ExtrautilitiesrebirthModBlocks.HALITOSIS_GENERATOR.get(), Items.DRAGON_BREATH),
                new Fuel(ExtrautilitiesrebirthModBlocks.NETHERSTAR_GENERATOR.get(), Items.NETHER_STAR),
                new Fuel(ExtrautilitiesrebirthModBlocks.OVERCLOCKED_GENERATOR.get(), Items.COAL),
                new Fuel(ExtrautilitiesrebirthModBlocks.PINK_GENERATOR.get(), Items.PINK_TULIP),
                new Fuel(ExtrautilitiesrebirthModBlocks.POTION_GENERATOR_1.get(), Items.POTION),
                new Fuel(ExtrautilitiesrebirthModBlocks.SLIMEY_GENERATOR.get(), Items.SLIME_BALL),
                new Fuel(ExtrautilitiesrebirthModBlocks.SURVIVAL_GENERATOR.get(), Items.COAL),
                new Fuel(ExtrautilitiesrebirthModBlocks.REDSTONE_GENERATOR.get(), Items.REDSTONE),
                new Fuel(ExtrautilitiesrebirthModBlocks.MAGMA_GENERATOR.get(), Items.LAVA_BUCKET));

        BlockPos missing = new BlockPos(0, 2, 0);
        for (int index = 0; index < fuels.size(); index++) {
            Fuel fuel = fuels.get(index);
            BlockPos pos = new BlockPos(index % 4 * 2, 2, index / 4 * 2);
            test.setBlock(pos, fuel.block());
            test.setBlock(pos.below(), ExtrautilitiesrebirthModBlocks.TRASH_CAN_ENERGY.get());
            var generator = (RandomizableContainerBlockEntity) test.getBlockEntity(pos);
            test.assertTrue(useWrench(test, player, wrench, pos).consumesAction(),
                    "Normal wrench use should link the fuel generator before it opens its menu");
            generator.load(generator.saveWithFullMetadata());
            if (fuel.item() == Items.LAVA_BUCKET) {
                var tank = generator.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
                tank.fill(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE);
            } else if (!pos.equals(missing)) {
                generator.setItem(0, new ItemStack(fuel.item()));
            }
        }

        test.runAfterDelay(5, () -> {
            test.assertTrue(core.getPersistentData().getString("running_gen").equals("15") && energy.getEnergyStored() == 0,
                    "One idle generator should prevent the other fifteen from starting the core");
            var generator = (RandomizableContainerBlockEntity) test.getBlockEntity(missing);
            generator.setItem(0, new ItemStack(Items.BAKED_POTATO));
        });
        test.runAfterDelay(10, () -> {
            test.assertTrue(core.getPersistentData().getBoolean("rgb_running") && energy.getEnergyStored() >= 25_000_000,
                    "All sixteen correctly linked generators should start the core");
            test.setBlock(missing, Blocks.AIR);
        });
        test.runAfterDelay(15, () -> {
            test.assertTrue(!core.getPersistentData().getBoolean("rgb_running") && energy.getEnergyStored() >= 25_000_000,
                    "The core should stop when a linked generator is removed and retain its stored power");
            test.succeed();
        });
    }

    private static InteractionResult useWrench(GameTestHelper test, Player player, ItemStack wrench, BlockPos pos) {
        BlockPos absolute = test.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return wrench.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static ItemStack craft(GameTestHelper test, ItemLike... ingredients) {
        var menu = new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        for (int slot = 0; slot < ingredients.length; slot++) {
            grid.setItem(slot, new ItemStack(ingredients[slot]));
        }
        var level = test.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                .map(recipe -> recipe.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private record Fuel(Block block, Item item) {
    }
}
