package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.ResonatorBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;

public class ResonantorGUIMenu extends PoweredProcessorMenu {
    private static final int[][] SLOT_POSITIONS = {{45, 34}, {98, 34}, {8, 19}};

    public ResonantorGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, extraData == null ? null : extraData.readBlockPos());
    }

    public ResonantorGUIMenu(int id, Inventory inventory, @Nullable BlockPos pos) {
        super(ExtrautilitiesrebirthModMenus.RESONANTOR_GUI.get(), id, inventory, pos, ResonatorBlockEntity.class, SLOT_POSITIONS);
    }
}
