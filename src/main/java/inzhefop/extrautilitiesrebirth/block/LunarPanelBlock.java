package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.block.entity.LunarPanelBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class LunarPanelBlock extends SkyPanelBlock {
    public LunarPanelBlock() {
        super(2);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LunarPanelBlockEntity(pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerRenderLayer() {
        ItemBlockRenderTypes.setRenderLayer(ExtrautilitiesrebirthModBlocks.LUNAR_PANEL.get(), type -> type == RenderType.cutout());
    }
}
