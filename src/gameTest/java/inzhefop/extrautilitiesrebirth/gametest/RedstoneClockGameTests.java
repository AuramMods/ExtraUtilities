package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class RedstoneClockGameTests {
    private static final BlockPos CLOCK = new BlockPos(3, 2, 3);
    private static final BlockPos COMPARATOR = CLOCK.west();

    @GameTest(template = "machine_test")
    public static void redstoneClockPulsesWithDirectAndComparatorLoads(GameTestHelper test) {
        test.setBlock(COMPARATOR.below(), Blocks.STONE);
        test.setBlock(COMPARATOR, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.EAST));
        test.setBlock(CLOCK, ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK.get());

        test.runAfterDelay(7, () -> assertSignal(test, true));
        test.runAfterDelay(25, () -> assertSignal(test, false));
        test.runAfterDelay(42, () -> {
            assertSignal(test, true);
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void clockResumesTheSavedPhaseAndKeepsForgeData(GameTestHelper test) {
        test.setBlock(CLOCK, ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK_OFF.get());
        var entity = test.getBlockEntity(CLOCK);
        entity.getPersistentData().putDouble("ticks", 14);
        entity.getPersistentData().putString("legacy_check", "retained");
        entity.load(entity.saveWithFullMetadata());

        test.runAfterDelay(2, () -> test.assertBlockPresent(ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK_OFF.get(), CLOCK));
        test.runAfterDelay(8, () -> {
            test.assertBlockPresent(ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK.get(), CLOCK);
            var active = test.getBlockEntity(CLOCK);
            test.assertTrue(active.getPersistentData().getString("legacy_check").equals("retained"),
                    "The phase transition should preserve saved custom data");
            test.succeed();
        });
    }

    private static void assertSignal(GameTestHelper test, boolean powered) {
        var block = powered ? ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK.get() : ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK_OFF.get();
        test.assertBlockPresent(block, CLOCK);
        var level = test.getLevel();
        var pos = test.absolutePos(CLOCK);
        int expected = powered ? 15 : 0;
        test.assertTrue(level.getSignal(pos, Direction.EAST) == expected, "Clock redstone signal should match its phase");
        var comparator = (ComparatorBlockEntity) test.getBlockEntity(COMPARATOR);
        int signal = comparator.getOutputSignal();
        test.assertTrue(signal == expected, "The adjacent comparator should emit " + expected + " at test tick "
                + test.getTick() + ", but emitted " + signal);
    }
}
