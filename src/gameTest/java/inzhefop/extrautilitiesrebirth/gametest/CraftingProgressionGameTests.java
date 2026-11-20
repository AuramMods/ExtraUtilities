package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(ExtrautilitiesrebirthMod.MODID)
@PrefixGameTestTemplate(false)
public class CraftingProgressionGameTests {
    private static final ResourceLocation FIELD_GUIDE = ResourceLocation.tryBuild(ExtrautilitiesrebirthMod.MODID, "field_guide");
    private static final ResourceLocation PATCHOULI_BOOK = ResourceLocation.tryBuild("patchouli", "guide_book");

    @GameTest(template = "machine_test")
    public static void bundledRecipesLoadAndCraftAtTheirVanillaWorkstations(GameTestHelper test) {
        var level = test.getLevel();
        var recipes = level.getRecipeManager();
        var files = level.getServer().getResourceManager().listResources("recipes", path ->
                path.getNamespace().equals(ExtrautilitiesrebirthMod.MODID) && path.getPath().endsWith(".json"));
        test.assertTrue(!files.isEmpty(), "The mod's recipe resources should be loaded");
        for (var file : files.keySet()) {
            var id = ResourceLocation.tryBuild(file.getNamespace(), file.getPath().substring("recipes/".length(), file.getPath().length() - ".json".length()));
            if (FIELD_GUIDE.equals(id) && !ModList.get().isLoaded("patchouli")) {
                test.assertTrue(recipes.byKey(id).isEmpty(), "The guide recipe should be absent without Patchouli");
                continue;
            }
            var recipe = recipes.byKey(id).orElseThrow(() -> new AssertionError("Recipe failed to load " + id));
            ItemStack result;
            if (recipe instanceof CraftingRecipe crafting) {
                var grid = new TransientCraftingContainer(new TestMenu(), 3, 3);
                var width = crafting instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
                for (int index = 0; index < crafting.getIngredients().size(); index++) {
                    var ingredient = crafting.getIngredients().get(index);
                    if (!ingredient.isEmpty()) {
                        var items = ingredient.getItems();
                        test.assertTrue(items.length > 0, "Unresolved ingredient in " + id);
                        grid.setItem(index % width + (index / width) * 3, items[0].copy());
                    }
                }
                result = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level)
                        .map(match -> match.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
            } else if (recipe instanceof SmeltingRecipe smelting) {
                var ingredient = smelting.getIngredients().get(0).getItems();
                test.assertTrue(ingredient.length > 0, "Unresolved ingredient in " + id);
                var input = new SimpleContainer(ingredient[0].copy());
                result = recipes.getRecipeFor(RecipeType.SMELTING, input, level)
                        .map(match -> match.assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
            } else {
                throw new AssertionError("Unhandled recipe in progression check " + id);
            }
            var expected = recipe.getResultItem(level.registryAccess());
            test.assertTrue(!result.isEmpty() && ItemStack.matches(result, expected),
                    "Recipe " + id + " produced " + result + " at its workstation, expected " + expected);
            if (FIELD_GUIDE.equals(id)) {
                test.assertTrue(PATCHOULI_BOOK.equals(ForgeRegistries.ITEMS.getKey(result.getItem()))
                                && result.hasTag() && FIELD_GUIDE.toString().equals(result.getTag().getString("patchouli:book")),
                        "The guide recipe should craft the Extra Utilities Guide");
            }
        }
        test.succeed();
    }

    private static class TestMenu extends AbstractContainerMenu {
        TestMenu() {
            super(null, 0);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
