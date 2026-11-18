package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.FurnaceBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;

public class FurnaceGUIMenu extends PoweredProcessorMenu {
    private static final int[][] SLOT_POSITIONS = {{44, 33}, {98, 33}, {8, 18}};

    public FurnaceGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, extraData == null ? null : extraData.readBlockPos());
    }

    public FurnaceGUIMenu(int id, Inventory inventory, @Nullable BlockPos pos) {
        super(ExtrautilitiesrebirthModMenus.FURNACE_GUI.get(), id, inventory, pos, FurnaceBlockEntity.class, SLOT_POSITIONS);
    }
}
