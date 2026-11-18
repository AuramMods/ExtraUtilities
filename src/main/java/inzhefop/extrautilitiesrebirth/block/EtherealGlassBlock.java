package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class EtherealGlassBlock extends SelectiveGlassBlock {
    public EtherealGlassBlock() {
        super(false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerRenderLayer() {
        ItemBlockRenderTypes.setRenderLayer(ExtrautilitiesrebirthModBlocks.ETHEREAL_GLASS.get(), type -> type == RenderType.translucent());
    }
}
