package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class GoldenLassoGameTests {
    @GameTest(template = "machine_test")
    public static void lassoCapturesAndReleasesSavedAnimal(GameTestHelper test) {
        Player player = test.makeMockPlayer();
        ItemStack empty = new ItemStack(ExtrautilitiesrebirthModItems.GOLDEN_LASSO.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        Sheep sheep = test.spawn(EntityType.SHEEP, new BlockPos(1, 2, 1));
        sheep.setCustomName(Component.literal("Bluebell"));
        sheep.setColor(DyeColor.BLUE);
        sheep.setAge(-1200);
        sheep.setHealth(4);
        var originalId = sheep.getUUID();
        test.assertTrue(player.interactOn(sheep, InteractionHand.MAIN_HAND).consumesAction() && sheep.isRemoved(),
                "Interacting with an animal must store it in the lasso");

        ItemStack filled = ItemStack.of(player.getMainHandItem().save(new CompoundTag()));
        test.assertTrue(filled.getMaxStackSize() == 1 && filled.getCount() == 1,
                "Only one lasso in the stack should hold the mob");
        test.assertTrue(player.getInventory().items.stream().anyMatch(s -> s.getCount() == 2 && s.getMaxStackSize() == 64),
                "The rest of the original stack should remain empty and usable");
        player.setItemInHand(InteractionHand.MAIN_HAND, filled);

        BlockPos support = new BlockPos(4, 1, 4);
        test.setBlock(support, Blocks.STONE);
        test.setBlock(support.above(), Blocks.STONE);
        test.assertTrue(!useLasso(test, player, support) && filled.getMaxStackSize() == 1,
                "A blocked release must keep the mob inside the lasso");
        test.setBlock(support.above(), Blocks.AIR);
        test.assertTrue(useLasso(test, player, support), "The saved animal should release into open space");

        List<Sheep> released = test.getLevel().getEntitiesOfClass(Sheep.class,
                new AABB(test.absolutePos(support.above())).inflate(0.2));
        test.assertTrue(released.size() == 1, "Release must spawn exactly one animal");
        Sheep restored = released.get(0);
        test.assertTrue(restored.getUUID().equals(originalId) && restored.getColor() == DyeColor.BLUE
                        && restored.getHealth() == 4 && restored.isBaby()
                        && restored.getName().getString().equals("Bluebell"),
                "The released animal must retain its identity, name, health, color, and age");
        test.assertTrue(filled.getMaxStackSize() == 64 && !useLasso(test, player, support),
                "A released lasso must return to the empty state");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void lassoCapturesVillagersButKeepsThemOutOfCrafting(GameTestHelper test) {
        var lasso = ExtrautilitiesrebirthModItems.GOLDEN_LASSO.get();
        Player player = test.makeMockPlayer();
        ItemStack empty = new ItemStack(lasso);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty.copy());
        var villager = test.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        test.assertTrue(player.interactOn(villager, InteractionHand.MAIN_HAND).consumesAction()
                        && villager.isRemoved(),
                "The lasso should capture a villager through normal interaction");
        ItemStack filled = player.getMainHandItem();
        for (String recipeId : List.of("chunk_loading_wand_rcp", "chicken_wing_ring_rcp")) {
            var recipe = test.getLevel().getRecipeManager().byKey(
                    new ResourceLocation("extrautilitiesrebirth", recipeId)).orElseThrow();
            test.assertTrue(recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(empty)),
                    "The empty lasso should remain a recipe ingredient");
            test.assertTrue(recipe.getIngredients().stream().noneMatch(ingredient -> ingredient.test(filled)),
                    "A filled lasso must not be accepted by the recipe");
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void goldenLassoDoesNotCaptureHostileMobs(GameTestHelper test) {
        Player player = test.makeMockPlayer();
        ItemStack lasso = new ItemStack(ExtrautilitiesrebirthModItems.GOLDEN_LASSO.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, lasso);
        var zombie = test.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        test.assertTrue(!player.interactOn(zombie, InteractionHand.MAIN_HAND).consumesAction()
                        && !zombie.isRemoved() && lasso.getMaxStackSize() == 64,
                "The Golden Lasso must leave a hostile mob in the world");
        test.succeed();
    }

    private static boolean useLasso(GameTestHelper test, Player player, BlockPos support) {
        BlockPos absolute = test.absolutePos(support);
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(absolute.above()), Direction.UP, absolute, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }
}
