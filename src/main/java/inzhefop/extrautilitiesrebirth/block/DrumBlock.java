package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.block.entity.DrumBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class DrumBlock extends Block implements EntityBlock {
    protected DrumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DrumBlockEntity drum) {
            drum.getCapability(ForgeCapabilities.FLUID_HANDLER, hit.getDirection()).ifPresent(tank -> {
                InteractionHand containerHand = hand;
                if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                        && FluidUtil.getFluidHandler(player.getOffhandItem()).isPresent()) {
                    containerHand = InteractionHand.OFF_HAND;
                }
                if (drum.isCreativeDrum() && player.getAbilities().instabuild && tank.getFluidInTank(0).isEmpty()) {
                    var sample = FluidUtil.getFluidContained(player.getItemInHand(containerHand));
                    if (sample.isPresent() && drum.configure(sample.get())) {
                        return;
                    }
                }
                if (!FluidUtil.interactWithFluidHandler(player, containerHand, tank)) {
                    FluidStack fluid = tank.getFluidInTank(0);
                    Component status = fluid.isEmpty() ? Component.translatable("message.extrautilitiesrebirth.drum.empty", tank.getTankCapacity(0))
                            : drum.isCreativeDrum() ? Component.translatable("message.extrautilitiesrebirth.drum.infinite", fluid.getDisplayName())
                            : Component.translatable("message.extrautilitiesrebirth.drum.stored", fluid.getDisplayName(),
                            fluid.getAmount(), tank.getTankCapacity(0));
                    player.displayClientMessage(status, true);
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (drops.isEmpty()) {
            drops = List.of(new ItemStack(this));
        }
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DrumBlockEntity drum)) {
            return drops;
        }
        var fluid = drum.saveCarriedFluid();
        if (fluid.isEmpty()) {
            return drops;
        }
        List<ItemStack> result = new ArrayList<>(drops);
        for (int index = 0; index < result.size(); index++) {
            var stack = result.get(index);
            if (stack.is(asItem())) {
                ItemStack filled = stack.copyWithCount(1);
                var data = BlockItem.getBlockEntityData(filled);
                if (data != null) {
                    data.put(DrumBlockEntity.FLUID_TAG, fluid.getCompound(DrumBlockEntity.FLUID_TAG));
                    fluid = data;
                }
                BlockItem.setBlockEntityData(filled, drum.getType(), fluid);
                result.set(index, filled);
                if (stack.getCount() > 1) {
                    ItemStack empty = stack.copyWithCount(stack.getCount() - 1);
                    var emptyData = BlockItem.getBlockEntityData(empty);
                    if (emptyData != null) {
                        emptyData.remove(DrumBlockEntity.FLUID_TAG);
                    }
                    result.add(empty);
                }
                break;
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        var tag = BlockItem.getBlockEntityData(stack);
        if (tag != null) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(DrumBlockEntity.FLUID_TAG));
            if (!fluid.isEmpty()) {
                tooltip.add(this instanceof CreativeDrumBlock
                        ? Component.translatable("message.extrautilitiesrebirth.drum.infinite", fluid.getDisplayName())
                        : Component.translatable("message.extrautilitiesrebirth.drum.carried", fluid.getDisplayName(), fluid.getAmount()));
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DrumBlockEntity drum ? drum.comparatorSignal() : 0;
    }
}
