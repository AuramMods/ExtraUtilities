package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.world.item.ItemStack;

final class MachineSpeed {
    private MachineSpeed() {
    }

    static double factorOf(ItemStack stack) {
        if (stack.is(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get())) {
            return 0.4;
        }
        if (stack.is(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get())) {
            return 0.5;
        }
        if (stack.is(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get())) {
            return 0.6;
        }
        return 0;
    }

    static double multiplier(ItemStack stack) {
        return Math.max(1, factorOf(stack) * stack.getCount());
    }
}
