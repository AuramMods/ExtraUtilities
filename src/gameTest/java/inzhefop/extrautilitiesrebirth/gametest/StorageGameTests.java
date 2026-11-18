package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.world.inventory.GoldenBagGUIMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class StorageGameTests {
    @GameTest(template = "machine_test")
    public static void goldenBagSharesContentsAndKeepsSaveData(GameTestHelper test) {
        var bag = new ItemStack(ExtrautilitiesrebirthModItems.GOLDEN_BAG_OF_HOLDING.get());
        var named = new ItemStack(Items.DIAMOND, 3);
        named.setHoverName(Component.literal("Saved diamonds"));
        test.assertTrue(bagInventory(bag).insertItem(53, named, false).isEmpty(),
                "The last of the 54 slots should accept items");
        test.assertTrue(ItemStack.matches(named, bagInventory(ItemStack.of(bag.save(new CompoundTag()))).getStackInSlot(53)),
                "A saved bag should retain its contents and names");

        var packet = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var copy = packet.writeItem(bag).readItem();
            test.assertTrue(ItemStack.matches(named, bagInventory(copy).getStackInSlot(53)),
                    "The multiplayer stack packet should include bag contents even when the item tag is empty");
            bag.setHoverName(Component.literal("Named Bag"));
            packet.clear();
            copy = packet.writeItem(bag).readItem();
            test.assertTrue(copy.getHoverName().getString().equals("Named Bag") && !bag.getTag().contains("Inventory"),
                    "Synchronizing inventory should keep the custom name and leave the source item tag alone");
        } finally {
            packet.release();
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void goldenBagMenuStaysBoundToTheOpeningStack(GameTestHelper test) {
        var player = test.makeMockPlayer();
        var inventory = player.getInventory();
        var bag = new ItemStack(ExtrautilitiesrebirthModItems.GOLDEN_BAG_OF_HOLDING.get());
        inventory.setItem(0, bag);
        inventory.setItem(9, new ItemStack(Items.IRON_INGOT, 12));
        var packet = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.writeBlockPos(player.blockPosition()).writeByte(0);
            var menu = new GoldenBagGUIMenu(1, inventory, packet);
            test.assertTrue(menu.stillValid(player) && !menu.getSlot(81).mayPickup(player),
                    "The opening bag should remain locked in its hotbar slot");
            test.assertTrue(menu.quickMoveStack(player, 54).getCount() == 12 && bagInventory(bag).getStackInSlot(0).getCount() == 12,
                    "Normal menu shift-click should store items in the bag");
            test.assertTrue(!menu.getSlot(0).mayPlace(bag.copy()), "Another bag should be rejected");
            menu.clicked(0, 0, ClickType.SWAP, player);
            test.assertTrue(inventory.getItem(0) == bag && bagInventory(bag).getStackInSlot(0).getCount() == 12,
                    "A hotbar swap should protect the opening bag");
            inventory.setItem(0, bag.copy());
            test.assertTrue(!menu.stillValid(player), "Replacing the held stack should close its original menu");
        } finally {
            packet.release();
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void goldChestKeepsNamedStacksAndValidatesItsMenu(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.GOLD_CHEST.get());
        var entity = test.getBlockEntity(pos);
        var player = test.makeMockPlayer();
        var center = Vec3.atCenterOf(test.absolutePos(pos));
        player.setPos(center.x, center.y, center.z);

        var menu = ((MenuProvider) entity).createMenu(1, player.getInventory(), player);
        var automation = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        var book = new ItemStack(Items.BOOK, 12);
        book.setHoverName(Component.literal("Named books"));
        test.assertTrue(automation.getSlots() == 54 && automation.insertItem(53, book, false).isEmpty(),
                "Automation should accept a named item in the last of the 54 slots");
        entity.load(entity.saveWithFullMetadata());
        test.assertTrue(ItemStack.matches(book, menu.getSlot(53).getItem()) && menu.stillValid(player),
                "The menu should see persisted data from the chest inventory");
        player.getInventory().setItem(9, new ItemStack(Items.BOOK, 4));
        test.assertTrue(menu.quickMoveStack(player, 54).getCount() == 4, "Shift-click should move books into the chest");
        test.assertTrue(menu.getSlot(0).getItem().getCount() == 4 && ItemStack.matches(book, menu.getSlot(53).getItem()),
                "Shift-click must keep a different name in a separate slot");

        player.setPos(center.x + 9, center.y, center.z);
        test.assertTrue(!menu.stillValid(player), "A player outside the normal chest range should lose access");
        player.setPos(center.x, center.y, center.z);
        test.setBlock(pos, Blocks.AIR);
        test.assertTrue(!menu.stillValid(player), "Removing the chest should close its menu");
        test.succeed();
    }

    private static IItemHandler bagInventory(ItemStack bag) {
        return bag.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
    }
}
