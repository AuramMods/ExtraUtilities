package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.world.inventory.EnchanterGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.function.Predicate;

public class EnchanterBlockEntity extends PoweredProcessorBlockEntity {
    private static final int UPGRADE_DIVISOR = 12;

    private static final class Recipes {
        private static final Map<Item, Recipe> BY_INPUT = Map.of(
                Blocks.BOOKSHELF.asItem(), new Recipe(stack -> stack.is(Items.LAPIS_LAZULI),
                        ExtrautilitiesrebirthModBlocks.MAGICAL_WOOD.get(), 1, 1, 1, 64_000, 1_600),
                Items.GOLD_INGOT, new Recipe(stack -> stack.is(Items.LAPIS_LAZULI),
                        ExtrautilitiesrebirthModItems.ENCHANTED_INGOT.get(), 1, 1, 1, 8_000, 200),
                Blocks.GOLD_BLOCK.asItem(), new Recipe(stack -> stack.is(Items.LAPIS_LAZULI),
                        ExtrautilitiesrebirthModBlocks.BLOCK_OF_ENCHANED_METAL.get(), 1, 9, 1, 24_000, 600),
                Blocks.IRON_BLOCK.asItem(), new Recipe(stack -> stack.is(Items.NETHER_STAR),
                        ExtrautilitiesrebirthModBlocks.BLOCK_OF_EVIL_INFUSED_IRON.get(), 8, 9, 8, 192_000, 4_800),
                Items.APPLE, new Recipe(stack -> stack.is(Items.LAPIS_LAZULI),
                        ExtrautilitiesrebirthModItems.MAGICAL_APPLE.get(), 16, 4, 16, 16_000, 400),
                Items.IRON_INGOT, new Recipe(stack -> stack.is(Items.NETHER_STAR),
                        ExtrautilitiesrebirthModItems.EVIL_INFUSED_IRON_INGOT.get(), 8, 1, 8, 24_000, 1_600),
                ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_UPGRADE_BASE.get().asItem(),
                        upgrade(Enchantments.SILK_TOUCH, 1, ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE.get()),
                ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SPEED_UPGRADE.get().asItem(),
                        upgrade(Enchantments.BLOCK_EFFICIENCY, 3, ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_2.get()),
                ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_2.get().asItem(),
                        upgrade(Enchantments.BLOCK_EFFICIENCY, 5, ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_3.get()));

        private static Recipe upgrade(Enchantment enchantment, int level, ItemLike output) {
            return new Recipe(stack -> EnchantmentHelper.getEnchantments(stack).getOrDefault(enchantment, 0) >= level,
                    output, 1, 1, 1, 64_000, 2_400);
        }
    }

    private record Recipe(Predicate<ItemStack> acceptsCatalyst, ItemLike result, int inputCount, int reagentCount,
                          int resultCount, int energy, int duration) {
    }

    public EnchanterBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.ENCHANTER.get(), pos, state, 4, 250_000, "progress", "enchanter", "Enchanter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EnchanterGUIMenu(id, inventory, worldPosition);
    }

    @Override
    protected Operation operation(int upgrades) {
        Item input = getItem(0).getItem();
        Recipe recipe = Recipes.BY_INPUT.get(input);
        if (recipe == null || !recipe.acceptsCatalyst().test(getItem(1))) {
            return null;
        }

        // An upgrade stack can exceed the maximum useful count for an expensive recipe.
        int effectiveUpgrades = Math.min(upgrades, energyCapacity() * UPGRADE_DIVISOR / recipe.energy());
        int cost = effectiveUpgrades == 0 ? recipe.energy() : recipe.energy() * effectiveUpgrades / UPGRADE_DIVISOR;
        return new Operation(BuiltInRegistries.ITEM.getKey(input).toString(), new ItemStack(recipe.result(), recipe.resultCount()),
                recipe.inputCount(), recipe.reagentCount(), cost, recipe.duration(), 1 + 1.54 * effectiveUpgrades);
    }

    @Override
    protected boolean acceptsIngredient(int slot, ItemStack stack) {
        if (slot == 0) {
            return Recipes.BY_INPUT.containsKey(stack.getItem());
        }
        if (stack.is(Items.LAPIS_LAZULI) || stack.is(Items.NETHER_STAR)) {
            return true;
        }
        var enchantments = EnchantmentHelper.getEnchantments(stack);
        return enchantments.getOrDefault(Enchantments.SILK_TOUCH, 0) >= 1
                || enchantments.getOrDefault(Enchantments.BLOCK_EFFICIENCY, 0) >= 3;
    }
}
