package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class CreativeUtilityGameTests {
    @GameTest(template = "machine_test")
    public static void creativeEnergyStorageSuppliesAllSixSidesWithoutDepletion(GameTestHelper test) {
        BlockPos pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.CREATIVE_ENERGY_STORAGE.get());
        var entity = test.getBlockEntity(pos);
        var legacy = entity.saveWithFullMetadata();
        legacy.putInt("energyStorage", 0);
        entity.load(legacy);

        for (Direction side : Direction.values()) {
            var source = entity.getCapability(ForgeCapabilities.ENERGY, side).orElseThrow(AssertionError::new);
            test.assertTrue(source.canExtract() && !source.canReceive(), "Every face should provide creative power");
            test.assertTrue(source.extractEnergy(Integer.MAX_VALUE, false) == Integer.MAX_VALUE
                            && source.extractEnergy(Integer.MAX_VALUE, false) == Integer.MAX_VALUE
                            && source.getEnergyStored() == Integer.MAX_VALUE,
                    "Extraction should remain unlimited even after loading a depleted legacy buffer");
            test.setBlock(pos.relative(side), ExtrautilitiesrebirthModBlocks.FURNACE.get());
        }

        test.succeedWhen(() -> {
            for (Direction side : Direction.values()) {
                var receiver = test.getBlockEntity(pos.relative(side)).getCapability(ForgeCapabilities.ENERGY, side.getOpposite())
                        .orElseThrow(AssertionError::new);
                test.assertTrue(receiver.getEnergyStored() == receiver.getMaxEnergyStored(),
                        "Adjacent machines should receive power on every side");
            }
        });
    }

    @GameTest(template = "machine_test")
    public static void creativeHarvestRemainsForOrdinaryBreaksAndDropsWhenSneaking(GameTestHelper test) {
        BlockPos pos = new BlockPos(3, 2, 3);
        var block = ExtrautilitiesrebirthModBlocks.CREATIVE_HARVEST.get();
        test.setBlock(pos, block);
        var level = test.getLevel();
        var state = level.getBlockState(test.absolutePos(pos));
        var player = test.makeMockPlayer();
        test.assertTrue(state.getDestroySpeed(level, test.absolutePos(pos)) == -1,
                "Creative Harvest should be unbreakable in survival");
        test.assertTrue(!state.onDestroyedByPlayer(level, test.absolutePos(pos), player, false, state.getFluidState()),
                "A normal creative break should leave the test block in place");
        test.assertBlockPresent(block, pos);
        test.assertTrue(test.getEntities(EntityType.ITEM, pos, 1).isEmpty(), "A normal creative break should drop nothing");

        player.setShiftKeyDown(true);
        test.assertTrue(state.onDestroyedByPlayer(level, test.absolutePos(pos), player, false, state.getFluidState()),
                "A sneaking creative break should remove the test block");
        test.assertBlockNotPresent(block, pos);
        var drops = test.getEntities(EntityType.ITEM, pos, 1);
        test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(block.asItem()),
                "Removing the test block should recover it as an item");
        test.succeed();
    }
}
