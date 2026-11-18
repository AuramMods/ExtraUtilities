package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class DrumGameTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    @GameTest(template = "machine_test")
    public static void drumTransfersBucketsInBothDirectionsWithoutPartialLoss(GameTestHelper test) {
        test.setBlock(POS, ExtrautilitiesrebirthModBlocks.STONE_DRUM.get());
        var player = test.makeMockPlayer();
        var tank = tank(test.getBlockEntity(POS), Direction.UP);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        test.useBlock(POS, player);
        test.assertTrue(player.getOffhandItem().is(Items.BUCKET) && tank.getFluidInTank(0).getAmount() == 1_000,
                "An offhand water bucket should fill the tank and return an empty bucket");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        test.useBlock(POS, player);
        test.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && tank.getFluidInTank(0).isEmpty(),
                "An empty bucket should withdraw one bucket of water");

        tank.fill(new FluidStack(Fluids.WATER, 15_500), FluidAction.EXECUTE);
        test.useBlock(POS, player);
        test.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && tank.getFluidInTank(0).getAmount() == 15_500,
                "A partly full tank should reject a bucket it cannot fit whole");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        test.useBlock(POS, player);
        test.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET) && tank.getFluidInTank(0).getFluid() == Fluids.WATER,
                "A drum should reject a different fluid");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void finiteDrumsExposeLegacyCapacityAndSaveTheirFluid(GameTestHelper test) {
        var blocks = List.of(ExtrautilitiesrebirthModBlocks.STONE_DRUM.get(), ExtrautilitiesrebirthModBlocks.IRON_DRUM.get(),
                ExtrautilitiesrebirthModBlocks.REINFORCED_DRUM.get(), ExtrautilitiesrebirthModBlocks.BEDROCK_DRUM.get());
        int[] capacities = {16_000, 256_000, 4_096_000, 65_536_000};
        for (int index = 0; index < blocks.size(); index++) {
            var block = blocks.get(index);
            var pos = new BlockPos(1 + index, 2, 3);
            test.setBlock(pos, block);
            BlockEntity entity = test.getBlockEntity(pos);
            IFluidHandler tank = tank(entity, Direction.NORTH);
            test.assertTrue(tank.fill(new FluidStack(Fluids.WATER, capacities[index] + 500), FluidAction.EXECUTE) == capacities[index],
                    "A sided fill should stop at the drum's capacity");
            var save = entity.saveWithFullMetadata();
            entity.load(save);
            test.assertTrue(tank(entity, Direction.DOWN).getFluidInTank(0).getAmount() == capacities[index],
                    "A full drum should retain its fluid after load");
            var level = test.getLevel();
            var absolute = test.absolutePos(pos);
            test.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute) == 15,
                    "A full drum should power a comparator at level 15");
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void breakingAndReplacingADrumKeepsItsFluid(GameTestHelper test) {
        var stoneDrum = ExtrautilitiesrebirthModBlocks.STONE_DRUM.get();
        test.setBlock(POS, stoneDrum);
        test.setBlock(POS.east().below(), Blocks.STONE);
        tank(test.getBlockEntity(POS), Direction.WEST).fill(new FluidStack(Fluids.LAVA, 2_750), FluidAction.EXECUTE);
        test.getLevel().destroyBlock(test.absolutePos(POS), true);
        var drops = test.getEntities(EntityType.ITEM, POS, 1);
        test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(stoneDrum.asItem()), "The drum should drop one item");
        var stack = drops.get(0).getItem().copy();
        drops.get(0).discard();
        var player = test.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos support = test.absolutePos(POS.east().below());
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        test.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "The carried drum should place normally");
        var stored = tank(test.getBlockEntity(POS.east()), Direction.NORTH).getFluidInTank(0);
        test.assertTrue(stored.getFluid() == Fluids.LAVA && stored.getAmount() == 2_750,
                "The placed drum should retain all of the original fluid");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void creativePlayerCanConfigureAnUnlimitedDrum(GameTestHelper test) {
        test.setBlock(POS, ExtrautilitiesrebirthModBlocks.CREATIVE_DRUM.get());
        var entity = test.getBlockEntity(POS);
        var tank = tank(entity, Direction.DOWN);
        var player = test.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        test.useBlock(POS, player);
        test.assertTrue(tank.getFluidInTank(0).isEmpty() && player.getMainHandItem().is(Items.WATER_BUCKET),
                "Survival use should leave an unconfigured creative drum empty");
        player.getAbilities().instabuild = true;
        test.useBlock(POS, player);
        test.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "Creative configuration should keep the sample bucket");
        entity.load(entity.saveWithFullMetadata());

        test.assertTrue(tank.drain(new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE).isEmpty(),
                "The configured drum should not supply a different fluid");
        for (int count = 0; count < 2; count++) {
            var drained = tank.drain(2_000_000, FluidAction.EXECUTE);
            test.assertTrue(drained.getFluid() == Fluids.WATER && drained.getAmount() == 2_000_000,
                    "The configured creative drum should provide its fluid indefinitely after load");
        }
        test.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE) == 0,
                "Automation should not change the configured fluid");
        test.succeed();
    }

    private static IFluidHandler tank(BlockEntity entity, Direction face) {
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER, face).orElseThrow(AssertionError::new);
    }
}
