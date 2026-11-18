package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.item.inventory.GoldenBagOfHoldingInventoryCapability;
import inzhefop.extrautilitiesrebirth.world.inventory.GoldenBagGUIMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public class GoldenBagOfHoldingItem extends Item {
    private static final String INVENTORY_TAG = "Inventory";

    public GoldenBagOfHoldingItem() {
        super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.COMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Immune to Fire").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            var provider = new SimpleMenuProvider((id, inventory, viewer) -> new GoldenBagGUIMenu(id, inventory, hand), stack.getHoverName());
            NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
                buffer.writeBlockPos(player.blockPosition());
                buffer.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag tag) {
        return new GoldenBagOfHoldingInventoryCapability();
    }

    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            if (handler instanceof ItemStackHandler inventory) {
                tag.put(INVENTORY_TAG, inventory.serializeNBT());
            }
        });
        return tag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
            super.readShareTag(stack, tag);
            return;
        }
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            if (handler instanceof ItemStackHandler inventory) {
                inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
            }
        });
        CompoundTag itemData = tag.copy();
        itemData.remove(INVENTORY_TAG);
        super.readShareTag(stack, itemData.isEmpty() ? null : itemData);
    }
}
