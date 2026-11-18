package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class SkyPanelGameTests {
    @GameTest(template = "machine_test")
    public static void panelsGenerateDuringTheirOwnSkyWindow(GameTestHelper test) {
        var level = test.getLevel();
        int elevation = Math.max(2, level.getSeaLevel() + 4 - test.absolutePos(BlockPos.ZERO).getY());
        var solar = new BlockPos(2, elevation, 3);
        var lunar = new BlockPos(4, elevation, 3);
        test.setBlock(solar, ExtrautilitiesrebirthModBlocks.SOLAR_PANEL.get());
        test.setBlock(lunar, ExtrautilitiesrebirthModBlocks.LUNAR_PANEL.get());
        IEnergyStorage sunlight = power(test, solar, Direction.DOWN);
        IEnergyStorage moonlight = power(test, lunar, Direction.DOWN);

        long originalTime = level.getDayTime();
        level.setDayTime(6_000);
        int[] daylight = new int[2];
        test.runAfterDelay(6, () -> {
            sunlight.extractEnergy(100_000, false);
            moonlight.extractEnergy(100_000, false);
        });
        test.runAfterDelay(16, () -> {
            daylight[0] = sunlight.getEnergyStored();
            daylight[1] = moonlight.getEnergyStored();
            level.setDayTime(18_000);
        });
        test.runAfterDelay(22, () -> {
            sunlight.extractEnergy(100_000, false);
            moonlight.extractEnergy(100_000, false);
        });
        test.runAfterDelay(32, () -> {
            int solarGain = sunlight.getEnergyStored();
            int lunarGain = moonlight.getEnergyStored();
            level.setDayTime(originalTime);
            test.assertTrue(daylight[0] == 100 && daylight[1] == 0, "Expected 100 solar FE and zero lunar FE in daylight, got "
                    + daylight[0] + " and " + daylight[1]);
            test.assertTrue(solarGain == 0 && lunarGain == 150, "Expected zero solar FE and 150 lunar FE at night, got "
                    + solarGain + " and " + lunarGain);
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void coveredPanelsKeepAndDeliverLegacyEnergy(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        var receiver = pos.west();
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.SOLAR_PANEL.get());
        test.setBlock(pos.above(), Blocks.STONE);
        test.setBlock(receiver, ExtrautilitiesrebirthModBlocks.FURNACE.get());
        var panel = test.getBlockEntity(pos);
        var legacy = panel.saveWithFullMetadata();
        legacy.putInt("energyStorage", 1_234);
        panel.load(legacy);
        test.assertTrue(power(test, pos, Direction.WEST).getEnergyStored() == 1_234,
                "Loaded panel should retain its stored FE");

        test.runAfterDelay(8, () -> {
            test.assertTrue(power(test, receiver, Direction.EAST).getEnergyStored() == 1_234,
                    "A shaded panel should deliver its stored energy without generating extra FE");
            test.assertTrue(power(test, pos, Direction.DOWN).getEnergyStored() == 0,
                    "Every FE accepted by the receiver should come out of the panel buffer");
            test.succeed();
        });
    }

    private static IEnergyStorage power(GameTestHelper test, BlockPos pos, Direction side) {
        return test.getBlockEntity(pos).getCapability(ForgeCapabilities.ENERGY, side).orElseThrow(AssertionError::new);
    }
}
