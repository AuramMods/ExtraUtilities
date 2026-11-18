package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.entity.TNTGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.world.inventory.NormalGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class GeneratorGameTests {
    private static final BlockPos GENERATOR = new BlockPos(3, 2, 3);

    @GameTest(template = "machine_test")
    public static void tntGeneratorExportsOnEachSide(GameTestHelper test) {
        placeTntGenerator(test);
        int tick = 0;
        for (Direction direction : Direction.values()) {
            BlockPos receiver = GENERATOR.relative(direction);
            test.runAfterDelay(++tick, () -> test.setBlock(receiver, ExtrautilitiesrebirthModBlocks.QUANTUM_QUARRY.get()));
            tick += 4;
            test.runAfterDelay(tick, () -> {
                test.assertTrue(energyOf(test.getBlockEntity(receiver)).getEnergyStored() > 0,
                        "Expected the " + direction + " receiver to gain energy");
                test.setBlock(receiver, Blocks.AIR);
            });
        }
        test.runAfterDelay(tick + 1, test::succeed);
    }

    @GameTest(template = "machine_test")
    public static void tntGeneratorReportsActualProductionAndBurnTime(GameTestHelper test) {
        TNTGeneratorBlockEntity generator = placeTntGenerator(test);
        test.runAfterDelay(4, () -> {
            test.assertTrue(generator.getItem(0).getCount() == 1, "Exactly one TNT should be consumed");
            test.assertTrue(energyOf(generator).getEnergyStored() > 0, "The TNT generator should store energy");
            test.assertTrue(generator.getPersistentData().getString("production").equals("Output: 200 FE/t"),
                    "The reported production should match unupgraded TNT generation");
            test.assertTrue(generator.getPersistentData().getString("seconds").equals("60s"),
                    "The reported time should come from the active burn");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void tntGeneratorRetainsEnergyTheReceiverCannotAccept(GameTestHelper test) {
        BlockPos receiver = GENERATOR.east();
        test.setBlock(receiver, ExtrautilitiesrebirthModBlocks.QUANTUM_QUARRY.get());
        IEnergyStorage sink = energyOf(test.getBlockEntity(receiver));
        sink.receiveEnergy(sink.getMaxEnergyStored(), false);
        TNTGeneratorBlockEntity generator = placeTntGenerator(test);

        test.runAfterDelay(4, () -> {
            test.assertTrue(sink.getEnergyStored() == sink.getMaxEnergyStored(), "The receiver should still be full");
            test.assertTrue(energyOf(generator).getEnergyStored() > 300,
                    "The generator must keep power rejected by a full receiver");
            sink.extractEnergy(300, false);
        });
        test.runAfterDelay(9, () -> {
            test.assertTrue(sink.getEnergyStored() == sink.getMaxEnergyStored(),
                    "The generator should fill the new capacity");
            test.assertTrue(energyOf(generator).getEnergyStored() > 0,
                    "The generator must keep the remainder");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void generatorRedstoneButtonControlsTheBurn(GameTestHelper test) {
        TNTGeneratorBlockEntity generator = placeTntGenerator(test);
        Player player = test.makeMockPlayer();
        BlockPos position = generator.getBlockPos();
        player.setPos(position.getX() + 1.5, position.getY(), position.getZ() + 0.5);
        NormalGUIMenu menu = (NormalGUIMenu) generator.createMenu(1, player.getInventory());
        test.assertTrue(menu.clickMenuButton(player, 0) && menu.requiresRedstone(),
                "The menu button should enable redstone control");

        test.runAfterDelay(4, () -> {
            test.assertTrue(generator.getItem(0).getCount() == 2 && energyOf(generator).getEnergyStored() == 0,
                    "The generator must wait for a redstone signal");
            test.setBlock(GENERATOR.east(), Blocks.REDSTONE_BLOCK);
        });
        test.runAfterDelay(8, () -> {
            test.assertTrue(generator.getItem(0).getCount() == 1 && energyOf(generator).getEnergyStored() > 0,
                    "The powered generator should burn its fuel");
            test.setBlock(GENERATOR.east(), Blocks.AIR);
            double burnTime = generator.getPersistentData().getDouble("time");
            test.runAfterDelay(4, () -> {
                test.assertTrue(generator.getPersistentData().getDouble("time") == burnTime,
                        "Losing the signal should pause the existing burn");
                test.assertTrue(menu.clickMenuButton(player, 0) && !menu.requiresRedstone(),
                        "The second click should allow the generator to run without redstone");
                test.runAfterDelay(4, () -> {
                    test.assertTrue(generator.getPersistentData().getDouble("time") < burnTime,
                            "Switching to ignore should resume the burn");
                    test.succeed();
                });
            });
        });
    }

    private static TNTGeneratorBlockEntity placeTntGenerator(GameTestHelper test) {
        test.setBlock(GENERATOR, ExtrautilitiesrebirthModBlocks.TNT_GENERATOR.get());
        TNTGeneratorBlockEntity generator = (TNTGeneratorBlockEntity) test.getBlockEntity(GENERATOR);
        generator.setItem(0, new ItemStack(Items.TNT, 2));
        return generator;
    }

    private static IEnergyStorage energyOf(BlockEntity entity) {
        return entity.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
    }
}
