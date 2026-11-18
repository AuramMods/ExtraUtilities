package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.world.inventory.ResonantorGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class ResonatorBlockEntity extends PoweredProcessorBlockEntity {
    private static final class Recipes {
        private static final Map<Item, Result> BY_INPUT = Map.of(
                ExtrautilitiesrebirthModBlocks.POLISHED_STONE.get().asItem(), new Result(ExtrautilitiesrebirthModBlocks.STONEBURNT.get(), 8_000),
                Blocks.SMOOTH_QUARTZ.asItem(), new Result(ExtrautilitiesrebirthModBlocks.QUARTZBURNT.get(), 8_000),
                ExtrautilitiesrebirthModBlocks.STONEBURNT.get().asItem(), new Result(ExtrautilitiesrebirthModBlocks.RAINBOW_STONE.get(), 64_000),
                Items.LAPIS_LAZULI, new Result(ExtrautilitiesrebirthModItems.LUNAR_REACTIVE_DUST.get(), 16_000),
                Items.COAL, new Result(ExtrautilitiesrebirthModItems.RED_COAL.get(), 16_000),
                Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.asItem(), new Result(ExtrautilitiesrebirthModItems.UPGRADE_BASE.get(), 8_000),
                Blocks.IRON_BARS.asItem(), new Result(ExtrautilitiesrebirthModItems.WIRELESS_RF_HEATING_COIL.get(), 16_000));
    }

    private record Result(ItemLike item, int energy) {
    }

    public ResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.RESONATOR.get(), pos, state, 3, 100_000, "ticks", "resonator", "Resonator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ResonantorGUIMenu(id, inventory, worldPosition);
    }

    @Override
    protected Operation operation(int upgrades) {
        Item item = getItem(0).getItem();
        Result recipe = Recipes.BY_INPUT.get(item);
        return recipe == null ? null
                : new Operation(BuiltInRegistries.ITEM.getKey(item).toString(), new ItemStack(recipe.item()), 1, 0, recipe.energy(), 100,
                1 + 1.5625 * upgrades);
    }

    @Override
    protected boolean acceptsIngredient(int slot, ItemStack stack) {
        return Recipes.BY_INPUT.containsKey(stack.getItem());
    }

}
