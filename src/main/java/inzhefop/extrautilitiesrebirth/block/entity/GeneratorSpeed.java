package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.world.item.ItemStack;

final class GeneratorSpeed {
    private GeneratorSpeed() {
    }

    static double multiplier(ItemStack stack) {
        if (stack.is(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get())) {
            return stack.getCount() * 1.05;
        }
        if (stack.is(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get())) {
            return stack.getCount() * 1.1;
        }
        if (stack.is(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get())) {
            return stack.getCount() * 1.2;
        }
        return 1;
    }
}
