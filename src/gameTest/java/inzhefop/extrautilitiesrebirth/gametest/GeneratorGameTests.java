package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.entity.MagmaGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.TNTGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.procedures.FuelGeneratorTick;
import inzhefop.extrautilitiesrebirth.world.inventory.NormalGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

import java.util.List;

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
    public static void tntGeneratorPausesWhenItsOwnBufferIsFull(GameTestHelper test) {
        TNTGeneratorBlockEntity generator = placeTntGenerator(test);
        IEnergyStorage energy = energyOf(generator);
        energy.receiveEnergy(99_990, false);

        test.runAfterDelay(5, () -> {
            test.assertTrue(generator.getItem(0).getCount() == 2 && energy.getEnergyStored() == 99_990,
                    "TNT should wait until there is space for a complete generation tick");
            energy.extractEnergy(200, false);
        });
        test.runAfterDelay(10, () -> {
            test.assertTrue(generator.getItem(0).getCount() == 1 && energy.getEnergyStored() == 99_990,
                    "TNT generation should resume when space becomes available");
            test.assertTrue(generator.getPersistentData().getDouble("time") == 1_199,
                    "A full buffer should preserve the remaining burn after the first tick");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void culinaryGeneratorRemembersTheFoodItIsBurning(GameTestHelper test) {
        var generator = placeWithFuel(test, GENERATOR, ExtrautilitiesrebirthModBlocks.CULINARY_GENERATOR.get(),
                new ItemStack(Items.BAKED_POTATO));
        test.runAfterDelay(5, () -> {
            test.assertTrue(generator.getItem(0).isEmpty(), "The generator should consume the potato");
            test.assertTrue(generator.getPersistentData().getString("production").equals("Output: 30 FE/t"),
                    "A single baked potato should continue to produce 30 FE/t after leaving the slot");
            generator.load(generator.saveWithFullMetadata());
            generator.setItem(0, new ItemStack(Items.CARROT));
        });
        test.runAfterDelay(10, () -> {
            test.assertTrue(generator.getItem(0).is(Items.CARROT)
                            && generator.getPersistentData().getString("production").equals("Output: 30 FE/t"),
                    "After reloading, the first burn should still use the potato's rate");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void generatorMenuRoutesFuelUpgradesAndOtherItems(GameTestHelper test) {
        test.setBlock(GENERATOR, ExtrautilitiesrebirthModBlocks.TNT_GENERATOR.get());
        var generator = (TNTGeneratorBlockEntity) test.getBlockEntity(GENERATOR);
        var player = test.makeMockPlayer();
        var inventory = player.getInventory();
        inventory.setItem(0, new ItemStack(Items.TNT, 2));
        inventory.setItem(1, new ItemStack(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get()));
        inventory.setItem(2, new ItemStack(Items.DIAMOND));
        NormalGUIMenu menu = (NormalGUIMenu) generator.createMenu(1, inventory);

        int hotbarStart = menu.slots.size() - 9;
        test.assertTrue(menu.quickMoveStack(player, hotbarStart).is(Items.TNT) && generator.getItem(0).getCount() == 2,
                "Shift-click should move TNT to the fuel slot");
        test.assertTrue(menu.quickMoveStack(player, hotbarStart + 1).is(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get())
                && generator.getItem(1).is(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get()),
                "Shift-click should route a speed upgrade to the upgrade slot");
        test.assertTrue(menu.quickMoveStack(player, hotbarStart + 2).is(Items.DIAMOND)
                        && inventory.getItem(9).is(Items.DIAMOND),
                "Shift-click should move other items between the hotbar and player inventory");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void specialtyGeneratorsUseTheirFixedFuelRates(GameTestHelper test) {
        assertGeneratorRates(test, List.of(
                new FuelCase(ExtrautilitiesrebirthModBlocks.DEATH_GENERATOR.get(), Items.BONE, 40),
                new FuelCase(ExtrautilitiesrebirthModBlocks.DISENCHANTMENT_GENERATOR.get(), Items.ENCHANTED_BOOK, 125),
                new FuelCase(ExtrautilitiesrebirthModBlocks.ENDER_GENERATOR.get(), Items.ENDER_EYE, 70),
                new FuelCase(ExtrautilitiesrebirthModBlocks.SLIMEY_GENERATOR.get(), Items.SLIME_BALL, 300),
                new FuelCase(ExtrautilitiesrebirthModBlocks.REDSTONE_GENERATOR.get(), Items.REDSTONE, 300)));
    }

    @GameTest(template = "machine_test")
    public static void furnaceFamilyUsesForgeFuelBurnTimes(GameTestHelper test) {
        assertGeneratorRates(test, List.of(
                new FuelCase(ExtrautilitiesrebirthModBlocks.FURNACE_GENERATOR.get(), Items.COAL, 30),
                new FuelCase(ExtrautilitiesrebirthModBlocks.OVERCLOCKED_GENERATOR.get(), Items.COAL, 60),
                new FuelCase(ExtrautilitiesrebirthModBlocks.SURVIVAL_GENERATOR.get(), Items.COAL, 30)));
    }

    @GameTest(template = "machine_test")
    public static void generatorsPreserveContainerRemainders(GameTestHelper test) {
        var furnace = placeWithFuel(test, new BlockPos(1, 2, 1), ExtrautilitiesrebirthModBlocks.FURNACE_GENERATOR.get(),
                new ItemStack(Items.LAVA_BUCKET));
        var potion = placeWithFuel(test, new BlockPos(4, 2, 1), ExtrautilitiesrebirthModBlocks.POTION_GENERATOR_1.get(),
                new ItemStack(Items.SPLASH_POTION));
        var halitosisPos = new BlockPos(1, 2, 4);
        var halitosis = placeWithFuel(test, halitosisPos, ExtrautilitiesrebirthModBlocks.HALITOSIS_GENERATOR.get(),
                new ItemStack(Items.DRAGON_BREATH, 2));

        test.runAfterDelay(5, () -> {
            var furnaceItems = furnace.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
                    .orElseThrow(AssertionError::new);
            test.assertTrue(furnaceItems.extractItem(0, 1, false).is(Items.BUCKET)
                            && energyOf(furnace).getEnergyStored() > 0,
                    "A hopper should be able to remove the spent furnace bucket");
            test.assertTrue(potion.getItem(0).is(Items.GLASS_BOTTLE) && energyOf(potion).getEnergyStored() > 0,
                    "The potion generator should keep the empty bottle in the fuel slot");
            test.assertTrue(halitosis.getItem(0).getCount() == 1 && energyOf(halitosis).getEnergyStored() > 0,
                    "The Halitosis Generator should keep the rest of a fuel stack");
            test.assertItemEntityCountIs(Items.GLASS_BOTTLE, halitosisPos, 1.5, 1);
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void netherStarGeneratorPreservesYieldWhenUpgradesExceedCapacity(GameTestHelper test) {
        var generator = placeWithFuel(test, GENERATOR, ExtrautilitiesrebirthModBlocks.NETHERSTAR_GENERATOR.get(),
                new ItemStack(Items.NETHER_STAR));
        generator.setItem(1, new ItemStack(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get(), 3));
        var energy = energyOf(generator);
        test.runAfterDelay(5, () -> {
            test.assertTrue(energy.getEnergyStored() == 100_000 && generator.getPersistentData().getDouble("time") == 2_399,
                    "The generator should fill its buffer and preserve the energy it could not store");
            energy.extractEnergy(100_000, false);
        });
        test.runAfterDelay(9, () -> {
            test.assertTrue(energy.getEnergyStored() == 100_000 && generator.getPersistentData().getDouble("time") == 2_398,
                    "The remaining energy should be available when the buffer drains");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void idleRainbowGeneratorExportsPreviouslyStoredEnergy(GameTestHelper test) {
        test.setBlock(GENERATOR, ExtrautilitiesrebirthModBlocks.RAINBOW_GENERATOR.get());
        var rainbow = test.getBlockEntity(GENERATOR);
        var energy = energyOf(rainbow);
        energy.receiveEnergy(200_000, false);
        rainbow.load(rainbow.saveWithFullMetadata());
        BlockPos receiver = GENERATOR.east();
        test.setBlock(receiver, ExtrautilitiesrebirthModBlocks.ENDER_QUARRY.get());
        var receiverEnergy = energyOf(test.getBlockEntity(receiver));
        int capacity = receiverEnergy.getMaxEnergyStored();
        receiverEnergy.receiveEnergy(capacity - 400, false);

        test.runAfterDelay(5, () -> {
            test.assertTrue(!rainbow.getPersistentData().getBoolean("rgb_running"),
                    "Without linked fuel generators, the rainbow generator should be idle");
            test.assertTrue(receiverEnergy.getEnergyStored() == capacity && energy.getEnergyStored() == 199_600,
                    "The idle generator should deliver stored energy and keep any excess");
            test.succeed();
        });
    }

    private record FuelCase(Block block, Item item, int rate) {
    }

    private static void assertGeneratorRates(GameTestHelper test, List<FuelCase> cases) {
        List<BlockPos> positions = List.of(new BlockPos(1, 2, 1), new BlockPos(4, 2, 1), new BlockPos(1, 2, 4),
                new BlockPos(4, 2, 4), GENERATOR);
        for (int index = 0; index < cases.size(); index++) {
            FuelCase fuelCase = cases.get(index);
            BlockPos pos = positions.get(index);
            test.setBlock(pos, fuelCase.block());
            var generator = (RandomizableContainerBlockEntity) test.getBlockEntity(pos);
            var items = generator.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                    .orElseThrow(AssertionError::new);
            test.assertTrue(!items.insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty(),
                    "The fuel slot should reject an item that cannot burn");
            test.assertTrue(items.insertItem(0, new ItemStack(fuelCase.item()), false).isEmpty(),
                    "The fuel slot should accept " + fuelCase.item());
            test.runAfterDelay(5, () -> test.assertTrue(generator.getItem(0).isEmpty()
                            && generator.getPersistentData().getString("production").equals("Output: " + fuelCase.rate() + " FE/t"),
                    "The generator should consume " + fuelCase.item() + " and run at " + fuelCase.rate() + " FE/t"));
        }
        test.runAfterDelay(6, test::succeed);
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

    @GameTest(template = "machine_test")
    public static void specialtyFuelAlternativesBurnThroughTheAutomationSlot(GameTestHelper test) {
        var death = ExtrautilitiesrebirthModBlocks.DEATH_GENERATOR.get();
        var frosty = ExtrautilitiesrebirthModBlocks.FROSTY_GENERATOR_1.get();
        var pink = ExtrautilitiesrebirthModBlocks.PINK_GENERATOR.get();
        var ender = ExtrautilitiesrebirthModBlocks.ENDER_GENERATOR.get();
        var slime = ExtrautilitiesrebirthModBlocks.SLIMEY_GENERATOR.get();
        var potion = ExtrautilitiesrebirthModBlocks.POTION_GENERATOR_1.get();
        var furnace = ExtrautilitiesrebirthModBlocks.FURNACE_GENERATOR.get();
        assertFuelGroup(test, death, 40, 600, List.of(Items.BONE, Items.BONE_BLOCK, Items.BONE_MEAL, Items.ROTTEN_FLESH));
        assertFuelGroup(test, frosty, 35, 1_200, List.of(Items.SNOW, Items.SNOW_BLOCK, Items.PACKED_ICE, Items.ICE, Items.SNOWBALL, Items.BLUE_ICE));
        assertFuelGroup(test, pink, 75, 300, List.of(Items.PINK_STAINED_GLASS, Items.PINK_GLAZED_TERRACOTTA, Items.PINK_CONCRETE,
                Items.PINK_CONCRETE_POWDER, Items.PINK_BANNER, Items.PINK_BED, Items.PINK_WOOL, Items.PINK_CARPET,
                Items.PINK_TERRACOTTA, Items.PINK_STAINED_GLASS_PANE, Items.PINK_SHULKER_BOX, Items.PINK_CANDLE, Items.PINK_DYE, Items.PINK_TULIP));
        assertFuelGroup(test, ender, 70, 1_200, List.of(Items.ENDER_PEARL, Items.ENDER_EYE));
        assertFuelGroup(test, slime, 300, 100, List.of(Items.SLIME_BALL, Items.SLIME_BLOCK));
        assertFuelGroup(test, potion, 40, 600, List.of(Items.POTION, Items.LINGERING_POTION, Items.SPLASH_POTION));
        assertFuelGroup(test, furnace, 30, 1_600, List.of(Items.COAL, Items.CHARCOAL));
        assertFuelGroup(test, furnace, 30, 11_200, List.of(ExtrautilitiesrebirthModItems.RED_COAL.get()));
        assertFuelGroup(test, furnace, 30, 3_200, List.of(ExtrautilitiesrebirthModBlocks.MAGICAL_PLANKS.get().asItem()));
        test.succeed();
    }

    @GameTest(template = "machine_test", timeoutTicks = 126)
    public static void allSpeedTiersPreserveTheExactYieldOfItemsAndLava(GameTestHelper test) {
        var testSlots = List.of(new BlockPos(1, 2, 1), new BlockPos(4, 2, 1), new BlockPos(1, 2, 4), new BlockPos(4, 2, 4));
        var speed = List.of(ItemStack.EMPTY, new ItemStack(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get()),
                new ItemStack(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get()), new ItemStack(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get()));
        var slimeRates = List.of(300, 315, 330, 360);
        var lavaRates = List.of(20, 21, 22, 24);
        for (int tier = 0; tier < speed.size(); tier++) {
            var slot = testSlots.get(tier);
            var slimey = placeWithFuel(test, slot, ExtrautilitiesrebirthModBlocks.SLIMEY_GENERATOR.get(), new ItemStack(Items.SLIME_BALL));
            slimey.setItem(1, speed.get(tier).copy());
            test.setBlock(slot.above(), ExtrautilitiesrebirthModBlocks.MAGMA_GENERATOR.get());
            var magma = (MagmaGeneratorBlockEntity) test.getBlockEntity(slot.above());
            magma.setItem(1, speed.get(tier).copy());
            for (var direction : Direction.values()) {
                test.assertTrue(isOutput(slimey, direction) && isOutput(magma, direction),
                        "A generator's exposed port should only emit energy on " + direction);
            }
            magmaTank(magma).fill(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE);
            int firstRate = slimeRates.get(tier);
            int secondRate = lavaRates.get(tier);
            test.runAfterDelay(5, () -> {
                test.assertTrue(slimey.getPersistentData().getString("production").equals("Output: " + firstRate + " FE/t"),
                        "The item generator should show the upgraded rate");
                test.assertTrue(magma.getPersistentData().getString("production").equals("Output: " + secondRate + " FE/t"),
                        "The Magma Generator should show the upgraded rate");
            });
            test.runAfterDelay(110, () -> {
                test.assertTrue(energyOf(slimey).getEnergyStored() == 30_000 && slimey.getPersistentData().getDouble("time") == 0,
                        "Every speed tier must preserve all 30,000 FE per slimeball after the burn");
                test.assertTrue(energyOf(magma).getEnergyStored() == 2_000 && magmaTank(magma).getFluidInTank(0).isEmpty(),
                        "Every speed tier must preserve all 2,000 FE per bucket after the burn");
            });
        }
        test.setBlock(GENERATOR, ExtrautilitiesrebirthModBlocks.RAINBOW_GENERATOR.get());
        var rainbow = test.getBlockEntity(GENERATOR);
        for (var direction : Direction.values()) {
            test.assertTrue(isOutput(rainbow, direction), "The Rainbow Generator's ports should only emit energy");
        }
        test.runAfterDelay(111, test::succeed);
    }

    private static boolean isOutput(BlockEntity generator, Direction direction) {
        var port = generator.getCapability(ForgeCapabilities.ENERGY, direction).orElseThrow(AssertionError::new);
        return port.canExtract() && !port.canReceive() && port.receiveEnergy(1, false) == 0;
    }

    private static void assertFuelGroup(GameTestHelper test, Block block, int rate, int duration, List<Item> fuels) {
        var level = test.getLevel();
        var pos = test.absolutePos(GENERATOR);
        for (var fuel : fuels) {
            test.setBlock(GENERATOR, block);
            var generator = test.getBlockEntity(GENERATOR);
            var items = generator.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
            test.assertTrue(items.insertItem(0, new ItemStack(fuel), false).isEmpty(), "The automation slot should accept " + fuel);
            FuelGeneratorTick.execute(level, pos);
            test.assertTrue(energyOf(generator).getEnergyStored() == rate && generator.getPersistentData().getInt("burnDuration") == duration,
                    "Wrong starting rate or duration for " + fuel);
            if (block == ExtrautilitiesrebirthModBlocks.POTION_GENERATOR_1.get()) {
                test.assertTrue(items.getStackInSlot(0).is(Items.GLASS_BOTTLE), "The potion container should return an empty bottle for " + fuel);
            }
            test.setBlock(GENERATOR, Blocks.AIR);
        }
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
