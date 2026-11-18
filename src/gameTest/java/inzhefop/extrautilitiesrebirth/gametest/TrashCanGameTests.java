package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class TrashCanGameTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    @GameTest(template = "machine_test")
    public static void trashCanDisposesMenuAndAutomationInputs(GameTestHelper test) {
        test.setBlock(POS, ExtrautilitiesrebirthModBlocks.TRASH_CAN.get());
        var entity = test.getBlockEntity(POS);
        var player = test.makeMockPlayer();
        var center = Vec3.atCenterOf(test.absolutePos(POS));
        player.setPos(center.x, center.y, center.z);
        var menu = ((MenuProvider) entity).createMenu(1, player.getInventory(), player);
        test.assertTrue(menu.stillValid(player), "The nearby player should be able to use the menu");
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 17));
        test.assertTrue(menu.quickMoveStack(player, 9).getCount() == 17 && player.getInventory().getItem(9).isEmpty(),
                "Shift-click should move the entire stack into the disposal slots");

        var sided = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        var unsided = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        test.assertTrue(sided.insertItem(8, new ItemStack(Items.DIRT, 2), false).isEmpty(),
                "Sided automation should be able to deposit items");
        test.assertTrue(sided.extractItem(8, 2, false).isEmpty() && unsided.extractItem(0, 17, false).isEmpty(),
                "Automation should never extract items deposited for disposal");

        test.runAfterDelay(3, () -> {
            test.assertTrue(menu.getSlot(0).getItem().isEmpty() && sided.getStackInSlot(8).isEmpty(),
                    "The server tick should dispose of inputs from both entry points");
            test.setBlock(POS, Blocks.AIR);
            test.assertTrue(!menu.stillValid(player), "Removing the Trash Can should invalidate its menu");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void fluidTrashCanDisposesBucketsAndPipedFluids(GameTestHelper test) {
        test.setBlock(POS, ExtrautilitiesrebirthModBlocks.TRASH_CAN_FLUID.get());
        var entity = test.getBlockEntity(POS);
        var tank = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).orElseThrow(AssertionError::new);
        var water = new FluidStack(Fluids.WATER, 1_234);
        var lava = new FluidStack(Fluids.LAVA, 4_321);
        test.assertTrue(tank.fill(water, FluidAction.SIMULATE) == water.getAmount() && tank.getFluidInTank(0).isEmpty(),
                "Fluid simulations should agree with an empty, writable sink");
        test.assertTrue(tank.fill(water, FluidAction.EXECUTE) == water.getAmount()
                        && tank.fill(lava, FluidAction.EXECUTE) == lava.getAmount(),
                "The sink should accept distinct fluids in the same tick");
        test.assertTrue(tank.getFluidInTank(0).isEmpty() && tank.drain(water, FluidAction.EXECUTE).isEmpty()
                        && tank.drain(4_321, FluidAction.SIMULATE).isEmpty(),
                "Disposed fluid should never be visible or drainable");
        var player = test.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        test.useBlock(POS, player);
        test.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Using a full bucket should dispose of its fluid");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void energyTrashCanOnlyAcceptsPower(GameTestHelper test) {
        test.setBlock(POS, ExtrautilitiesrebirthModBlocks.TRASH_CAN_ENERGY.get());
        var entity = test.getBlockEntity(POS);
        for (Direction side : Direction.values()) {
            var power = entity.getCapability(ForgeCapabilities.ENERGY, side).orElseThrow(AssertionError::new);
            test.assertTrue(power.canReceive() && !power.canExtract(), "Every face should be an input only");
            test.assertTrue(power.receiveEnergy(Integer.MAX_VALUE, true) == Integer.MAX_VALUE
                            && power.receiveEnergy(Integer.MAX_VALUE, false) == Integer.MAX_VALUE,
                    "Simulated and actual FE transfers should agree");
            test.assertTrue(power.getEnergyStored() == 0 && power.extractEnergy(1, false) == 0,
                    "The sink should dispose of FE immediately");
        }

        var legacy = entity.saveWithFullMetadata();
        legacy.putInt("energyStorage", 10_000);
        entity.load(legacy);
        var power = entity.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
        test.assertTrue(power.getEnergyStored() == 0, "Legacy contents waiting for disposal should not become a power source");
        test.succeed();
    }
}
