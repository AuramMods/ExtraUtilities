package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class CompressedBlockGameTests {
    @GameTest(template = "machine_test")
    public static void allEightCobblestoneTiersCraftAndDrop(GameTestHelper test) {
        verifyFamily(test, Blocks.COBBLESTONE, ExtrautilitiesrebirthModBlocks.COMPRESSED_COBBLESTONE.get(),
                ExtrautilitiesrebirthModBlocks.DOUBLE_COMPRESSED_COBBLESTONE.get(), ExtrautilitiesrebirthModBlocks.TRIPLE_COMPRESSED_COBBLESTONE.get(),
                ExtrautilitiesrebirthModBlocks.QUADRUPLE_COMPRESSED_COBBLESTONE.get(), ExtrautilitiesrebirthModBlocks.QUINTUPLE_COMPRESSED_COBBLESTONE.get(),
                ExtrautilitiesrebirthModBlocks.SEXTUPLE_COMPRESSED_COBBLESTONE.get(), ExtrautilitiesrebirthModBlocks.SEPTUPLE_COMPRESSED_COBBLESTONE.get(),
                ExtrautilitiesrebirthModBlocks.OCTUPLE_COMPRESSED_COBBLESTONE.get());
    }

    @GameTest(template = "machine_test")
    public static void allFourDirtTiersCraftAndDrop(GameTestHelper test) {
        verifyFamily(test, Blocks.DIRT, ExtrautilitiesrebirthModBlocks.COMPRESSED_DIRT.get(),
                ExtrautilitiesrebirthModBlocks.DOUBLE_COMPRESSED_DIRT.get(), ExtrautilitiesrebirthModBlocks.TRIPLE_COMPRESSED_DIRT.get(),
                ExtrautilitiesrebirthModBlocks.QUADRUPLE_COMPRESSED_DIRT.get());
    }

    @GameTest(template = "machine_test")
    public static void bothSandTiersCraftAndDrop(GameTestHelper test) {
        verifyFamily(test, Blocks.SAND, ExtrautilitiesrebirthModBlocks.COMPRESSED_SAND.get(),
                ExtrautilitiesrebirthModBlocks.DOUBLE_COMPRESSED_SAND.get());
    }

    @GameTest(template = "machine_test")
    public static void bothGravelTiersCraftAndDrop(GameTestHelper test) {
        verifyFamily(test, Blocks.GRAVEL, ExtrautilitiesrebirthModBlocks.COMPRESSED_GRAVEL.get(),
                ExtrautilitiesrebirthModBlocks.DOUBLE_COMPRESSED_GRAVEL.get());
    }

    private static void verifyFamily(GameTestHelper test, Block material, Block... tiers) {
        var place = new BlockPos(3, 2, 3);
        test.setBlock(place.below(), Blocks.STONE);
        var support = test.absolutePos(place.below());
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        var player = test.makeMockPlayer();
        for (Block tier : tiers) {
            ItemStack compressed = craft(test, material, 9);
            test.assertTrue(compressed.is(tier.asItem()) && compressed.getCount() == 1,
                    "Nine " + material.getName().getString() + " blocks should craft one " + tier.getName().getString());

            ItemStack decompressed = craft(test, tier, 1);
            test.assertTrue(decompressed.is(material.asItem()) && decompressed.getCount() == 9,
                    tier.getName().getString() + " should unpack into nine " + material.getName().getString() + " blocks");

            player.setItemInHand(InteractionHand.MAIN_HAND, compressed);
            test.assertTrue(compressed.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                    "The crafted block should place normally");
            test.assertBlockPresent(tier, place);
            test.getLevel().destroyBlock(test.absolutePos(place), true);
            var drops = test.getEntities(EntityType.ITEM, place, 1);
            test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(tier.asItem()) && drops.get(0).getItem().getCount() == 1,
                    "The placed compressed block should drop itself once");
            drops.get(0).discard();

            material = tier;
        }
        test.succeed();
    }

    private static ItemStack craft(GameTestHelper test, Block ingredient, int occupiedSlots) {
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
        for (int slot = 0; slot < occupiedSlots; slot++) {
            grid.setItem(slot, new ItemStack(ingredient));
        }
        var level = test.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                .map(recipe -> recipe.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
    }
}
