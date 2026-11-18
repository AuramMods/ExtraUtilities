package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.world.inventory.FurnaceGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;

public class FurnaceBlockEntity extends PoweredProcessorBlockEntity {
    private final RecipeManager.CachedCheck<Container, SmeltingRecipe> smelting = RecipeManager.createCheck(RecipeType.SMELTING);

    public FurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.FURNACE.get(), pos, state, 3, 1_000_000, "smelt", "furnace", "Furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FurnaceGUIMenu(id, inventory, worldPosition);
    }

    @Override
    protected Operation operation(int upgrades) {
        ItemStack stack = getItem(0);
        if (stack.isEmpty()) {
            return null;
        }
        Container input = new SimpleContainer(stack.copy());
        return smelting.getRecipeFor(input, level).map(recipe -> {
            ItemStack result = recipe.assemble(input, level.registryAccess());
            return result.isEmpty() ? null
                    : new Operation(recipe.getId().toString(), result, 1, 0, 1_000 * (1 + upgrades), 100,
                    1 + Math.round(1.55 * upgrades));
        }).orElse(null);
    }

    @Override
    protected boolean acceptsIngredient(int slot, ItemStack stack) {
        return level != null && smelting.getRecipeFor(new SimpleContainer(stack.copy()), level).isPresent();
    }

}
