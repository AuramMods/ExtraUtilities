package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class IneffableGlassBlock extends SelectiveGlassBlock {
    public IneffableGlassBlock() {
        super(false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerRenderLayer() {
        ItemBlockRenderTypes.setRenderLayer(ExtrautilitiesrebirthModBlocks.INEFFABLE_GLASS.get(), type -> type == RenderType.translucent());
    }
}
