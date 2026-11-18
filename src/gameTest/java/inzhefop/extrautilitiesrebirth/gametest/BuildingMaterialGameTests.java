package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class BuildingMaterialGameTests {
    @GameTest(template = "machine_test")
    public static void buildingMaterialRecipesWorkAtACraftingGrid(GameTestHelper test) {
        var ingot = ExtrautilitiesrebirthModItems.BEDROCKIUM_INGOT.get();
        var crafts = List.of(
                new Craft("bedrock_rcp", ExtrautilitiesrebirthModBlocks.BEDROCK_COBBLESTONE.get(), 1),
                new Craft("bedrock_bricks_rcp", ExtrautilitiesrebirthModBlocks.BEDROCK_BRICKS.get(), 4),
                new Craft("bedrock_polished_rcp", ExtrautilitiesrebirthModBlocks.BEDROCK_SLABS.get(), 4),
                new Craft("bedingot_1_rcp", ingot, 9),
                new Craft("bedingot_2_rcp", ingot, 9),
                new Craft("bedingot_3_rcp", ingot, 9),
                new Craft("border_stone_rcp", ExtrautilitiesrebirthModBlocks.BORDER_STONE.get(), 4),
                new Craft("chisled_stone_rcp", ExtrautilitiesrebirthModBlocks.CHISLED_STONE_BRICKS.get(), 4),
                new Craft("edged_stone_bricks_rcp", ExtrautilitiesrebirthModBlocks.EDGED_STONE_BRICKS.get(), 4),
                new Craft("polished_stone_rcp", ExtrautilitiesrebirthModBlocks.POLISHED_STONE.get(), 4),
                new Craft("magical_wood_rcp", ExtrautilitiesrebirthModBlocks.MAGICAL_WOOD.get(), 1),
                new Craft("magical_planks_rcp", ExtrautilitiesrebirthModBlocks.MAGICAL_PLANKS.get(), 4));

        var level = test.getLevel();
        var recipes = level.getRecipeManager();
        for (Craft craft : crafts) {
            var recipeId = ResourceLocation.tryBuild("extrautilitiesrebirth", craft.id());
            var recipe = (CraftingRecipe) recipes.byKey(recipeId).orElseThrow(() -> new AssertionError("Missing " + recipeId));
            var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
                @Override
                public ItemStack quickMoveStack(Player player, int index) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            }, 3, 3);
            int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            for (int index = 0; index < recipe.getIngredients().size(); index++) {
                var ingredient = recipe.getIngredients().get(index);
                if (!ingredient.isEmpty()) {
                    grid.setItem(index % width + (index / width) * 3, ingredient.getItems()[0].copy());
                }
            }
            var result = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level)
                    .map(match -> match.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
            test.assertTrue(result.is(craft.output().asItem()) && result.getCount() == craft.count(), recipeId + " produced the wrong item or count");
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void buildingBlocksAndCreativeSamplePlaceAndDrop(GameTestHelper test) {
        var blocks = List.of(ExtrautilitiesrebirthModBlocks.BEDROCK_COBBLESTONE.get(), ExtrautilitiesrebirthModBlocks.BEDROCK_BRICKS.get(),
                ExtrautilitiesrebirthModBlocks.BEDROCK_SLABS.get(), ExtrautilitiesrebirthModBlocks.BORDER_STONE.get(),
                ExtrautilitiesrebirthModBlocks.CHISLED_STONE_BRICKS.get(), ExtrautilitiesrebirthModBlocks.EDGED_STONE_BRICKS.get(),
                ExtrautilitiesrebirthModBlocks.POLISHED_STONE.get(), ExtrautilitiesrebirthModBlocks.QUARTZBURNT.get(),
                ExtrautilitiesrebirthModBlocks.RAINBOW_STONE.get(), ExtrautilitiesrebirthModBlocks.STONEBURNT.get(),
                ExtrautilitiesrebirthModBlocks.MAGICAL_WOOD.get(), ExtrautilitiesrebirthModBlocks.MAGICAL_PLANKS.get(),
                ExtrautilitiesrebirthModBlocks.FERROUS_JUNIPER_LOG.get(), ExtrautilitiesrebirthModBlocks.FERROUS_JUNIPER_PLANKS.get(),
                ExtrautilitiesrebirthModBlocks.CREATIVE_INV_SAMPLE.get());

        var pos = new BlockPos(3, 2, 3);
        test.setBlock(pos.below(), Blocks.STONE);
        var support = test.absolutePos(pos.below());
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        var player = test.makeMockPlayer();
        player.setYRot(90);
        var level = test.getLevel();
        var actual = test.absolutePos(pos);
        for (Block block : blocks) {
            ItemStack stack = new ItemStack(block);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            test.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                    "The block should be placeable: " + block.getName().getString());
            test.assertBlockPresent(block, pos);
            var state = level.getBlockState(actual);
            var facing = HorizontalDirectionalBlock.FACING;
            if (state.hasProperty(facing)) {
                test.assertTrue(state.getValue(facing) == Direction.EAST && state.rotate(Rotation.CLOCKWISE_90).getValue(facing) == Direction.SOUTH,
                        "The block should face the player and rotate with the rest of the structure");
            }
            if (block == ExtrautilitiesrebirthModBlocks.MAGICAL_WOOD.get()) {
                test.assertTrue(state.getEnchantPowerBonus(level, actual) == 2.5f, "Magical Wood should provide its bookcase bonus");
            }
            level.destroyBlock(actual, true);
            var drops = test.getEntities(EntityType.ITEM, pos, 1);
            test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(block.asItem()), "The block should drop itself");
            drops.get(0).discard();
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void ferrousJuniperRequiresAnAxeOfTheDeclaredTier(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        var level = test.getLevel();
        var actual = test.absolutePos(pos);
        var player = test.makeMockPlayer();
        for (Block block : List.of(ExtrautilitiesrebirthModBlocks.FERROUS_JUNIPER_LOG.get(), ExtrautilitiesrebirthModBlocks.FERROUS_JUNIPER_PLANKS.get())) {
            test.setBlock(pos, block);
            var state = level.getBlockState(actual);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            test.assertTrue(!state.canHarvestBlock(level, actual, player), "Ferrous Juniper should require an axe");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_AXE));
            test.assertTrue(!state.canHarvestBlock(level, actual, player), "A wooden axe should be below the specified tier");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_AXE));
            test.assertTrue(state.canHarvestBlock(level, actual, player), "A stone axe should harvest Ferrous Juniper");
        }
        test.succeed();
    }

    private record Craft(String id, ItemLike output, int count) {
    }
}
