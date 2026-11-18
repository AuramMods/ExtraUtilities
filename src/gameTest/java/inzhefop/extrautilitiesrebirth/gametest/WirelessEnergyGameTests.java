package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class WirelessEnergyGameTests {
    @GameTest(template = "machine_test")
    public static void legacyWirelessLinksDeliverTheEntireBuffer(GameTestHelper test) {
        var battery = new BlockPos(2, 2, 3);
        var full = battery.west(2);
        var receiver = battery.east(2);
        test.setBlock(battery, ExtrautilitiesrebirthModBlocks.WIRELESS_FE_BATTERY.get());
        test.setBlock(full, ExtrautilitiesrebirthModBlocks.FE_TRANSMITTER.get());
        test.setBlock(receiver, ExtrautilitiesrebirthModBlocks.FURNACE.get());

        var blockEntity = test.getBlockEntity(battery);
        var nbt = blockEntity.saveWithFullMetadata();
        var legacy = new CompoundTag();
        legacy.putDouble("connections", 2);
        putLegacyPosition(legacy, 1, test.absolutePos(full));
        putLegacyPosition(legacy, 2, test.absolutePos(receiver));
        nbt.put("ForgeData", legacy);
        nbt.putInt("energyStorage", 3);
        blockEntity.load(nbt);

        var transmitter = test.getBlockEntity(full).getCapability(ForgeCapabilities.ENERGY, Direction.WEST)
                .orElseThrow(AssertionError::new);
        test.assertTrue(transmitter.receiveEnergy(3_200, false) == 3_200, "The transmitter should accept wired FE");

        test.runAfterDelay(8, () -> {
            test.assertTrue(power(test, receiver).getEnergyStored() == 3 && power(test, battery).getEnergyStored() == 0,
                    "The battery should deliver all three FE even when another link is full");
            test.assertTrue(transmitter.extractEnergy(200, false) == 200, "The transmitter should supply wired FE");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void wrenchLinksAtZeroHeightAndKeepsOneConnection(GameTestHelper test) {
        int y = -test.absolutePos(BlockPos.ZERO).getY();
        var battery = new BlockPos(2, y, 3);
        var receiver = battery.east(2);
        test.setBlock(battery, Blocks.AIR);
        test.setBlock(receiver, Blocks.AIR);
        test.setBlock(battery, ExtrautilitiesrebirthModBlocks.WIRELESS_FE_BATTERY.get());
        test.setBlock(receiver, ExtrautilitiesrebirthModBlocks.FE_TRANSMITTER.get());
        var player = test.makeMockPlayer();
        var wrench = new ItemStack(ExtrautilitiesrebirthModItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        test.assertTrue(useWrench(test, player, wrench, receiver), "Right-clicking a receiver at Y=0 should select it");
        test.assertTrue(useWrench(test, player, wrench, battery), "Right-clicking the battery should link the receiver");
        test.assertTrue(useWrench(test, player, wrench, battery), "A repeated click should acknowledge the existing connection");
        var entity = test.getBlockEntity(battery);
        test.assertTrue(entity.getPersistentData().getInt("connections") == 1, "One receiver should take one link slot");
        entity.load(entity.saveWithFullMetadata());
        power(test, battery).receiveEnergy(1_234, false);

        test.succeedWhen(() -> {
            int received = power(test, receiver).getEnergyStored();
            int buffered = power(test, battery).getEnergyStored();
            test.assertTrue(received == 1_234 && buffered == 0,
                    "Reloaded link should deliver 1234 FE, but the receiver has " + received + " and the battery holds " + buffered);
            player.setShiftKeyDown(true);
            test.assertTrue(useWrench(test, player, wrench, battery) && entity.getPersistentData().getInt("connections") == 0,
                    "Sneak-right-click with the same receiver selected should unlink it");
            test.setBlock(battery, Blocks.AIR);
            test.setBlock(receiver, Blocks.AIR);
        });
    }

    @GameTest(template = "machine_test")
    public static void wirelessLinksRespectTheRangeAndTenReceiverLimit(GameTestHelper test) {
        var battery = new BlockPos(3, 2, 3);
        var atRangeBoundary = battery.above(10);
        test.setBlock(battery, ExtrautilitiesrebirthModBlocks.WIRELESS_FE_BATTERY.get());
        test.setBlock(atRangeBoundary, ExtrautilitiesrebirthModBlocks.FE_TRANSMITTER.get());
        var player = test.makeMockPlayer();
        var wrench = new ItemStack(ExtrautilitiesrebirthModItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        useWrench(test, player, wrench, atRangeBoundary);
        useWrench(test, player, wrench, battery);
        var data = test.getBlockEntity(battery).getPersistentData();
        test.assertTrue(data.getInt("connections") == 0, "A receiver ten blocks away is outside the range");

        for (int index = 0; index < 11; index++) {
            var receiver = new BlockPos(1 + index % 5, 2, 1 + index / 5);
            test.setBlock(receiver, ExtrautilitiesrebirthModBlocks.FE_TRANSMITTER.get());
            useWrench(test, player, wrench, receiver);
            useWrench(test, player, wrench, battery);
        }
        test.assertTrue(data.getInt("connections") == 10, "The eleventh receiver must leave the ten existing links intact");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void transmitterRetainsStoredEnergyAndInvalidatesCachedPorts(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.FE_TRANSMITTER.get());
        var entity = test.getBlockEntity(pos);
        var capability = entity.getCapability(ForgeCapabilities.ENERGY, Direction.NORTH);
        var storage = capability.orElseThrow(AssertionError::new);
        test.assertTrue(storage.receiveEnergy(4_000, false) == 3_200, "The transmitter should stop at its legacy capacity");
        entity.load(entity.saveWithFullMetadata());
        test.assertTrue(power(test, pos).getEnergyStored() == 3_200, "Saved energy should remain available through the unsided port");
        test.setBlock(pos, Blocks.AIR);
        test.assertTrue(!capability.isPresent(), "Removing the transmitter should invalidate cached energy ports");
        test.succeed();
    }

    private static boolean useWrench(GameTestHelper test, Player player, ItemStack wrench, BlockPos pos) {
        BlockPos actual = test.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(actual), Direction.UP, actual, false);
        return wrench.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }

    private static IEnergyStorage power(GameTestHelper test, BlockPos pos) {
        return test.getBlockEntity(pos).getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
    }

    private static void putLegacyPosition(CompoundTag tag, int index, BlockPos pos) {
        tag.putDouble("connection" + index + "x", pos.getX());
        tag.putDouble("connection" + index + "y", pos.getY());
        tag.putDouble("connection" + index + "z", pos.getZ());
    }
}
