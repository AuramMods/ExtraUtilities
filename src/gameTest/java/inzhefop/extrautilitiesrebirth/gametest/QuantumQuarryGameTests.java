package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.block.QuantumQuarryActuatorBlock;
import inzhefop.extrautilitiesrebirth.block.entity.QuantumQuarryBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.world.inventory.EnderQuarryGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class QuantumQuarryGameTests {
    private static final BlockPos QUARRY = new BlockPos(3, 2, 3);
    private static final int POWER = 1_000_000;

    @GameTest(template = "machine_test", timeoutTicks = 800)
    public static void quantumQuarryMinesThroughActuatorsAndPreservesBlockedOutput(GameTestHelper test) {
        test.assertTrue(test.getLevel().getServer().getLevel(QuantumQuarryBlockEntity.MINING_DIMENSION) != null,
                "The mining dimension must load from the mod's world data");
        test.setBlock(QUARRY, ExtrautilitiesrebirthModBlocks.QUANTUM_QUARRY.get());
        var quarry = (QuantumQuarryBlockEntity) test.getBlockEntity(QUARRY);

        placeActuator(test, Direction.EAST);
        IEnergyStorage input = test.getBlockEntity(QUARRY.east())
                .getCapability(ForgeCapabilities.ENERGY, Direction.EAST).orElseThrow(AssertionError::new);
        test.assertTrue(input.receiveEnergy(POWER, false) == 0, "An incomplete assembly must reject actuator power");
        for (Direction arm : Direction.values()) {
            if (arm != Direction.EAST) {
                placeActuator(test, arm);
            }
        }

        BlockPos output = QUARRY.relative(Direction.EAST, 2);
        test.setBlock(output, Blocks.CHEST);
        var chest = (ChestBlockEntity) test.getBlockEntity(output);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        test.assertTrue(input.receiveEnergy(POWER, false) == POWER && !input.canExtract(),
                "Assembled actuators should accept power for the quarry");

        ItemStack[] buffered = {ItemStack.EMPTY};
        test.startSequence()
                .thenWaitUntil(() -> test.assertTrue(!quarry.getItem(0).isEmpty(),
                        "An actuator-powered quarry should harvest generated terrain and buffer its drops"))
                .thenExecute(() -> {
                    test.assertTrue(input.getEnergyStored() < POWER, "Mining must consume stored FE");
                    buffered[0] = quarry.getItem(0).copy();
                    var snapshot = quarry.saveWithFullMetadata();
                    atLeastOneChunkWasScanned(test, snapshot.getLong("MiningCursor"));
                    quarry.load(snapshot.copy());
                    test.assertTrue(ItemStack.matches(quarry.getItem(0), buffered[0])
                                    && quarry.saveWithFullMetadata().getLong("MiningCursor") == snapshot.getLong("MiningCursor"),
                            "The output and terrain cursor must survive serialization");
                    chest.clearContent();
                })
                .thenWaitUntil(() -> {
                    int received = 0;
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                        ItemStack stack = chest.getItem(slot);
                        if (ItemStack.isSameItemSameTags(stack, buffered[0])) {
                            received += stack.getCount();
                        }
                    }
                    test.assertTrue(received >= buffered[0].getCount(),
                            "The quarry must deliver its buffered drops when the chest has room");
                })
                .thenExecute(() -> {
                    test.setBlock(QUARRY.above(), Blocks.AIR);
                    test.assertTrue(!input.canReceive(), "Removing an actuator must disable power through the others");
                })
                .thenSucceed();
    }

    @GameTest(template = "machine_test")
    public static void quantumQuarryMenuAcceptsMiningBooksAndProvidesRecipes(GameTestHelper test) {
        test.setBlock(QUARRY, ExtrautilitiesrebirthModBlocks.QUANTUM_QUARRY.get());
        for (Direction arm : Direction.values()) {
            placeActuator(test, arm);
        }
        var quarry = (QuantumQuarryBlockEntity) test.getBlockEntity(QUARRY);
        Player player = test.makeMockPlayer();
        BlockPos position = quarry.getBlockPos();
        player.setPos(position.getX() + 2.5, position.getY(), position.getZ() + 0.5);
        var menu = (EnderQuarryGUIMenu) quarry.createMenu(1, player.getInventory());
        ItemStack miningBook = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SILK_TOUCH, 1));
        player.getInventory().setItem(0, miningBook);
        test.assertTrue(menu.stillValid(player) && menu.getSlot(3).mayPlace(miningBook)
                        && !menu.getSlot(3).mayPlace(new ItemStack(Items.DIAMOND)),
                "The assembled quarry menu must accept only mining books as upgrades");
        test.assertTrue(!menu.quickMoveStack(player, 31).isEmpty()
                        && quarry.getItem(3).is(Items.ENCHANTED_BOOK)
                        && player.getInventory().getItem(0).isEmpty(),
                "Shift-clicking a book should install it into the existing fourth slot");
        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "The output is read-only");
        test.setBlock(QUARRY.above(), Blocks.AIR);
        test.assertTrue(!menu.stillValid(player), "The menu should close when its assembly is broken");
        for (String id : new String[] {"quantum_quarry", "quantum_quarry_actuator"}) {
            test.assertTrue(test.getLevel().getRecipeManager().byKey(new ResourceLocation("extrautilitiesrebirth", id)).isPresent(),
                    "Missing survival recipe for " + id);
        }
        test.succeed();
    }

    private static void placeActuator(GameTestHelper test, Direction arm) {
        test.setBlock(QUARRY.relative(arm), ExtrautilitiesrebirthModBlocks.QUANTUM_QUARRY_ACTUATOR.get()
                .defaultBlockState().setValue(QuantumQuarryActuatorBlock.FACING, arm));
    }

    private static void atLeastOneChunkWasScanned(GameTestHelper test, long cursor) {
        test.assertTrue(cursor > 0, "The saved terrain cursor should move when a block is mined");
    }
}
