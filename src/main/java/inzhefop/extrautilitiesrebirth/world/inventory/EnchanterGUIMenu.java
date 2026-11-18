package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.EnchanterBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;

public class EnchanterGUIMenu extends PoweredProcessorMenu {
    private static final int[][] SLOT_POSITIONS = {{36, 35}, {56, 35}, {108, 35}, {8, 19}};

    public EnchanterGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, extraData == null ? null : extraData.readBlockPos());
    }

    public EnchanterGUIMenu(int id, Inventory inventory, @Nullable BlockPos pos) {
        super(ExtrautilitiesrebirthModMenus.ENCHANTER_GUI.get(), id, inventory, pos, EnchanterBlockEntity.class, SLOT_POSITIONS);
    }
}
