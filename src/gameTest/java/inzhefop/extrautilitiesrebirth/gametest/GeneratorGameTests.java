package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.entity.MagmaGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.TNTGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.world.inventory.NormalGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
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

    @GameTest(template = "machine_test")
    public static void specialtyGeneratorsUseTheirFuelTags(GameTestHelper test) {
        Block frosty = ExtrautilitiesrebirthModBlocks.FROSTY_GENERATOR_1.get();
        Block pink = ExtrautilitiesrebirthModBlocks.PINK_GENERATOR.get();
        var ice = placeWithFuel(test, new BlockPos(1, 2, 1), frosty, new ItemStack(Items.ICE, 2));
        var snowball = placeWithFuel(test, new BlockPos(4, 2, 1), frosty, new ItemStack(Items.SNOWBALL, 2));
        var wool = placeWithFuel(test, new BlockPos(1, 2, 4), pink, new ItemStack(Items.PINK_WOOL, 2));
        var untagged = placeWithFuel(test, new BlockPos(4, 2, 4), pink, new ItemStack(Items.WHITE_WOOL, 2));

        test.runAfterDelay(5, () -> {
            test.assertTrue(ice.getItem(0).getCount() == 1 && energyOf(ice).getEnergyStored() > 0,
                    "Frosty Generator should burn ice from its fuel tag");
            test.assertTrue(snowball.getItem(0).getCount() == 1 && energyOf(snowball).getEnergyStored() > 0,
                    "Frosty Generator should keep accepting snowballs");
            test.assertTrue(wool.getItem(0).getCount() == 1 && energyOf(wool).getEnergyStored() > 0,
                    "Pink Generator should burn pink wool from its fuel tag");
            test.assertTrue(untagged.getItem(0).getCount() == 2 && energyOf(untagged).getEnergyStored() == 0,
                    "Pink Generator must leave untagged wool intact");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test", timeoutTicks = 120)
    public static void magmaGeneratorBurnsOneBucketThroughAFullCycle(GameTestHelper test) {
        test.setBlock(GENERATOR, ExtrautilitiesrebirthModBlocks.MAGMA_GENERATOR.get());
        BlockEntity generator = test.getBlockEntity(GENERATOR);
        IFluidHandler tank = magmaTank(generator);
        test.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE) == 0,
                "The fuel tank should reject water");
        test.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "Pipes should be able to fill the lava tank");

        test.runAfterDelay(5, () -> {
            test.assertTrue(tank.getFluidInTank(0).isEmpty(), "The generator should consume one bucket of lava");
            test.assertTrue(generator.getPersistentData().getString("production").equals("Output: 20 FE/t"),
                    "The generator should report lava generation");
            generator.load(generator.saveWithFullMetadata());
        });
        test.runAfterDelay(105, () -> {
            test.assertTrue(energyOf(generator).getEnergyStored() == 2_000,
                    "One lava bucket should generate the full 2,000 FE through a save and load");
            test.assertTrue(generator.getPersistentData().getDouble("time") == 0,
                    "The burn should end once the lava is used");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void magmaGeneratorKeepsLavaWhenStorageIsFull(GameTestHelper test) {
        test.setBlock(GENERATOR, ExtrautilitiesrebirthModBlocks.MAGMA_GENERATOR.get());
        BlockEntity generator = test.getBlockEntity(GENERATOR);
        IFluidHandler tank = magmaTank(generator);
        IEnergyStorage energy = energyOf(generator);
        energy.receiveEnergy(energy.getMaxEnergyStored() - 10, false);
        Player player = test.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        test.useBlock(GENERATOR, player);
        test.assertTrue(player.getMainHandItem().is(Items.BUCKET) && tank.getFluidInTank(0).getAmount() == 1_000,
                "Using a lava bucket on the generator should add the lava and return an empty bucket");
        tank.fill(new FluidStack(Fluids.LAVA, 6_500), IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        test.useBlock(GENERATOR, player);
        test.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET) && tank.getFluidInTank(0).getAmount() == 7_500,
                "A bucket must stay in the player's hand if the entire bucket cannot fit");

        test.runAfterDelay(5, () -> {
            test.assertTrue(tank.getFluidInTank(0).getAmount() == 7_500 && energy.getEnergyStored() == 99_990,
                    "The generator should wait for room to store a complete production tick");
            energy.extractEnergy(20, false);
        });
        test.runAfterDelay(10, () -> {
            test.assertTrue(tank.getFluidInTank(0).getAmount() == 6_500 && energy.getEnergyStored() == 99_990,
                    "One tick should generate power when there is space");
            test.assertTrue(generator.getPersistentData().getDouble("time") == 99,
                    "The active burn should pause again when the buffer fills");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void magmaGeneratorSupportsOffhandBucketsAndSpeedUpgrades(GameTestHelper test) {
        Block block = ExtrautilitiesrebirthModBlocks.MAGMA_GENERATOR.get();
        test.setBlock(GENERATOR, block);
        var generator = (MagmaGeneratorBlockEntity) test.getBlockEntity(GENERATOR);
        BlockPos pos = test.absolutePos(GENERATOR);
        Player player = test.makeMockPlayer();
        player.getAbilities().instabuild = true;
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LAVA_BUCKET));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        test.assertTrue(generator.getBlockState().use(test.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                == InteractionResult.PASS, "The main hand should defer to the lava bucket in the off hand");
        generator.getBlockState().use(test.getLevel(), player, InteractionHand.OFF_HAND, hit);
        test.assertTrue(player.getOffhandItem().is(Items.LAVA_BUCKET) && magmaTank(generator).getFluidInTank(0).getAmount() == 1_000,
                "Creative use should add lava without changing the offhand bucket");

        var handler = generator.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        var upgrade = new ItemStack(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get(), 3);
        test.assertTrue(!handler.insertItem(0, upgrade, false).isEmpty()
                        && !handler.insertItem(1, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                "Automation should reject items in the lava slot and non-upgrades in the upgrade slot");
        test.assertTrue(handler.insertItem(1, upgrade, false).isEmpty(), "Automation should accept a valid upgrade");
        NormalGUIMenu menu = (NormalGUIMenu) generator.createMenu(1, player.getInventory());
        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.LAVA_BUCKET))
                && menu.getSlot(1).mayPlace(new ItemStack(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get())),
                "The menu should enforce the same slot rules as automation");

        test.runAfterDelay(5, () -> test.assertTrue(
                generator.getPersistentData().getString("production").equals("Output: 72 FE/t"),
                "Three super speed upgrades should generate 72 FE per full tick"));
        test.runAfterDelay(35, () -> {
            test.assertTrue(energyOf(generator).getEnergyStored() == 2_000,
                    "The faster cycle should still generate exactly 2,000 FE");
            test.succeed();
        });
    }

    private static IFluidHandler magmaTank(BlockEntity generator) {
        return generator.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(AssertionError::new);
    }

    private static RandomizableContainerBlockEntity placeWithFuel(GameTestHelper test, BlockPos pos,
                                                                  Block block, ItemStack fuel) {
        test.setBlock(pos, block);
        var generator = (RandomizableContainerBlockEntity) test.getBlockEntity(pos);
        generator.setItem(0, fuel);
        return generator;
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
