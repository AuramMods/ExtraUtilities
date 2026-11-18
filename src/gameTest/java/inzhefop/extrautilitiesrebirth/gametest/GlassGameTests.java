package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class GlassGameTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    @GameTest(template = "machine_test")
    public static void selectiveGlassUsesPlayerSneakAndMobCollisionRules(GameTestHelper test) {
        var glasses = List.of(ExtrautilitiesrebirthModBlocks.ETHEREAL_GLASS.get(), ExtrautilitiesrebirthModBlocks.DARK_ETHEREAL_GLASS.get(),
                ExtrautilitiesrebirthModBlocks.INEFFABLE_GLASS.get(), ExtrautilitiesrebirthModBlocks.INEFFABLE_INVERTED_GLASS.get());
        var player = test.makeMockPlayer();
        var pig = test.spawn(EntityType.PIG, POS.east());
        var level = test.getLevel();
        var actual = test.absolutePos(POS);
        for (Block block : glasses) {
            test.setBlock(POS, block);
            var state = level.getBlockState(actual);
            boolean inverted = block == ExtrautilitiesrebirthModBlocks.INEFFABLE_INVERTED_GLASS.get();
            player.setShiftKeyDown(false);
            test.assertTrue(state.getCollisionShape(level, actual, CollisionContext.of(player)).isEmpty() != inverted,
                    block.getName().getString() + " should apply its standing-player passage rule");
            player.setShiftKeyDown(true);
            test.assertTrue(state.getCollisionShape(level, actual, CollisionContext.of(player)).isEmpty() == inverted,
                    block.getName().getString() + " should reverse passage when the player sneaks");
            test.assertTrue(state.getCollisionShape(level, actual, CollisionContext.of(pig)).isEmpty() == inverted,
                    block.getName().getString() + " should apply its mob passage rule");
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void allGlassVariantsKeepTheirLightAndDrops(GameTestHelper test) {
        var glasses = List.of(
                new Glass(ExtrautilitiesrebirthModBlocks.SANDY_GLASS.get(), 15),
                new Glass(ExtrautilitiesrebirthModBlocks.CREEPER_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.DARK_ETHEREAL_GLASS.get(), 15),
                new Glass(ExtrautilitiesrebirthModBlocks.DARK_GLASS.get(), 15),
                new Glass(ExtrautilitiesrebirthModBlocks.EDGED_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.ETHEREAL_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.CLEAN_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.GLASS_BRICKS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.GLOWSTONE_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.GOLDEN_EDGED_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.HEART_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.INEFFABLE_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.INEFFABLE_INVERTED_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.OBSIDIAN_GLASS.get(), 4),
                new Glass(ExtrautilitiesrebirthModBlocks.REINFORCED_DARK_GLASS.get(), 15),
                new Glass(ExtrautilitiesrebirthModBlocks.SQUARE_GLASS.get(), 0),
                new Glass(ExtrautilitiesrebirthModBlocks.SWIRLING_GLASS.get(), 0));

        var level = test.getLevel();
        var actual = test.absolutePos(POS);
        for (Glass glass : glasses) {
            Block block = glass.block();
            String name = block.getName().getString();
            test.setBlock(POS, block);
            var state = level.getBlockState(actual);
            test.assertTrue(state.getLightBlock(level, actual) == glass.opacity(), name + " should keep its declared light opacity");
            int emission = block == ExtrautilitiesrebirthModBlocks.GLOWSTONE_GLASS.get() ? 15 : 0;
            test.assertTrue(state.getLightEmission(level, actual) == emission, name + " should keep its declared light emission");
            test.assertTrue(state.skipRendering(state, Direction.NORTH), name + " should cull faces against matching glass");
            level.destroyBlock(actual, true);
            var drops = test.getEntities(EntityType.ITEM, POS, 1);
            test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(block.asItem()) && drops.get(0).getItem().getCount() == 1,
                    name + " should drop one matching block");
            drops.get(0).discard();
        }
        test.succeed();
    }

    private record Glass(Block block, int opacity) {
    }
}
