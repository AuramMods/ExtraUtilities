package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.entity.EnderThermicPumpBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class EnderThermicPumpGameTests {
    private static final BlockPos MARKER = new BlockPos(5, 3, 5);
    private static final BlockPos PUMP = MARKER.east();
    private static final BlockPos TOP_SOURCE = MARKER.north().west().below();

    @GameTest(template = "machine_test", timeoutTicks = 160)
    public static void startsFromMarkersAndPumpsBelowZeroThroughARealDrain(GameTestHelper test) {
        var pump = prepareMarkers(test);
        var marker = ExtrautilitiesrebirthModBlocks.ENDER_MARKER.get();
        test.useBlock(PUMP);
        test.assertTrue(pump.getPersistentData().getInt("state") == 0, "An incomplete marker boundary must leave the pump idle");
        test.setBlock(MARKER.north(3).west(3), marker);
        test.useBlock(MARKER);

        BlockPos deepSource = TOP_SOURCE.below();
        encloseSource(test, deepSource, Blocks.WATER);
        test.assertTrue(test.absolutePos(deepSource).getY() < 0, "The source should be below Y=0");
        BlockPos stairs = TOP_SOURCE.north().west();
        test.setBlock(stairs, Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        BlockPos outside = MARKER.east().north().below(2);
        encloseSource(test, outside, Blocks.WATER);

        var drum = ExtrautilitiesrebirthModBlocks.STONE_DRUM.get();
        test.setBlock(PUMP.above(), drum);
        var receiver = tankOf(test.getBlockEntity(PUMP.above()));
        receiver.fill(new FluidStack(Fluids.WATER, 15_500), IFluidHandler.FluidAction.EXECUTE);
        test.useBlock(PUMP);
        var energy = energyOf(pump);
        energy.receiveEnergy(900_000, false);

        test.succeedWhen(() -> {
            test.assertTrue(pump.getPersistentData().getInt("state") == 2, "The linked pump should finish scanning its full rectangle");
            test.assertBlockPresent(Blocks.STONE, deepSource);
            test.assertBlockPresent(Blocks.OAK_STAIRS, stairs);
            test.assertTrue(test.getBlockState(stairs).getValue(BlockStateProperties.WATERLOGGED),
                    "The pump should leave waterlogged blocks intact");
            test.assertBlockPresent(Blocks.WATER, outside);
            test.assertTrue(receiver.getFluidInTank(0).getAmount() == 16_000
                            && tankOf(pump).getFluidInTank(0).getAmount() == 500,
                    "The pump should preserve the part of the 1,000 mB source rejected by the receiver");
            test.assertTrue(energy.getEnergyStored() < 900_000, "The pump should use Forge Energy");
        });
    }

    @GameTest(template = "machine_test", timeoutTicks = 110)
    public static void fullTankKeepsLavaSourceUntilItCanFitAfterReload(GameTestHelper test) {
        var pump = prepareMarkers(test);
        test.setBlock(MARKER.north(3).west(3), ExtrautilitiesrebirthModBlocks.ENDER_MARKER.get());
        test.useBlock(MARKER);
        test.setBlock(PUMP.north(), ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_WORLD_HOLE_UPGRADE.get());
        encloseSource(test, TOP_SOURCE, Blocks.LAVA);
        var tank = tankOf(pump);
        tank.fill(new FluidStack(Fluids.LAVA, 63_500), IFluidHandler.FluidAction.EXECUTE);
        var energy = energyOf(pump);
        energy.receiveEnergy(900_000, false);
        test.useBlock(PUMP);

        test.runAfterDelay(25, () -> {
            test.assertBlockPresent(Blocks.LAVA, TOP_SOURCE);
            test.assertTrue(pump.getPersistentData().getInt("state") == 3 && tank.getFluidInTank(0).getAmount() == 63_500,
                    "A partial bucket of capacity should block without removing the lava source");
            test.assertTrue(energy.getEnergyStored() == 900_000, "The blocked pump should preserve stored energy");
            pump.load(pump.saveWithFullMetadata());
            tank.drain(500, IFluidHandler.FluidAction.EXECUTE);
        });
        test.runAfterDelay(55, () -> {
            test.assertBlockPresent(Blocks.AIR, TOP_SOURCE);
            test.assertTrue(tank.getFluidInTank(0).getFluid() == Fluids.LAVA && tank.getFluidInTank(0).getAmount() == 64_000,
                    "After reload and drainage, the whole bucket should fit and World Hole should leave air");
            test.succeed();
        });
    }

    private static EnderThermicPumpBlockEntity prepareMarkers(GameTestHelper test) {
        var marker = ExtrautilitiesrebirthModBlocks.ENDER_MARKER.get();
        for (BlockPos pos : List.of(MARKER, MARKER.north(3), MARKER.west(3))) {
            test.setBlock(pos, marker);
        }
        test.useBlock(MARKER);
        test.setBlock(PUMP, ExtrautilitiesrebirthModBlocks.ENDER_THERMIC_PUMP.get());
        test.setBlock(PUMP.south(), ExtrautilitiesrebirthModBlocks.ENDER_QUARRY_SILK_TOUCH_UPGRADE_3.get());
        return (EnderThermicPumpBlockEntity) test.getBlockEntity(PUMP);
    }

    private static void encloseSource(GameTestHelper test, BlockPos pos, Block fluid) {
        for (Direction side : Direction.values()) {
            if (side != Direction.UP) {
                test.setBlock(pos.relative(side), Blocks.STONE);
            }
        }
        test.setBlock(pos, fluid);
    }

    private static IFluidHandler tankOf(BlockEntity entity) {
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
    }

    private static IEnergyStorage energyOf(BlockEntity entity) {
        return entity.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
    }
}
