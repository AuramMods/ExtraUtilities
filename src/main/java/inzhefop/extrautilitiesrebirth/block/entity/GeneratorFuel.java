package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeHooks;

public enum GeneratorFuel {
    CULINARY(600, 6),
    DEATH(600, 40),
    DISENCHANTMENT(600, 125),
    ENDER(1_200, 70),
    TNT(1_200, 200),
    FROSTY(1_200, 35),
    FURNACE(0, 30),
    HALITOSIS(12_000, 35),
    NETHERSTAR(2_400, 100_000),
    OVERCLOCKED(0, 60),
    PINK(300, 75),
    POTION(600, 40),
    SLIMEY(100, 300),
    SURVIVAL(0, 30),
    REDSTONE(100, 300);

    private static final TagKey<Item> DEATH_ITEMS = ItemTags.create(new ResourceLocation("forge", "death_gen_tag"));
    private static final TagKey<Item> FROSTY_ITEMS = ItemTags.create(new ResourceLocation("forge", "frosty_gen_tag"));
    private static final TagKey<Item> PINK_ITEMS = ItemTags.create(new ResourceLocation("forge", "pink_items"));

    private final int duration;
    private final int basePower;

    GeneratorFuel(int duration, int basePower) {
        this.duration = duration;
        this.basePower = basePower;
    }

    public int burnDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return switch (this) {
            case FURNACE, OVERCLOCKED, SURVIVAL -> ForgeHooks.getBurnTime(stack, null);
            case CULINARY -> nutrition(stack) > 0 ? duration : 0;
            case DEATH -> stack.is(DEATH_ITEMS) ? duration : 0;
            case DISENCHANTMENT -> stack.is(Items.ENCHANTED_BOOK) ? duration : 0;
            case ENDER -> stack.is(Items.ENDER_EYE) || stack.is(Items.ENDER_PEARL) ? duration : 0;
            case TNT -> stack.is(Items.TNT) ? duration : 0;
            case FROSTY -> stack.is(FROSTY_ITEMS) ? duration : 0;
            case HALITOSIS -> stack.is(Items.DRAGON_BREATH) ? duration : 0;
            case NETHERSTAR -> stack.is(Items.NETHER_STAR) ? duration : 0;
            case PINK -> stack.is(PINK_ITEMS) ? duration : 0;
            case POTION -> stack.is(Items.POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.SPLASH_POTION)
                    ? duration : 0;
            case SLIMEY -> stack.is(Items.SLIME_BLOCK) || stack.is(Items.SLIME_BALL) ? duration : 0;
            case REDSTONE -> stack.is(Items.REDSTONE) ? duration : 0;
        };
    }

    public int defaultDuration() {
        return duration;
    }

    public int power(ItemStack stack) {
        // Legacy saves did not store the consumed food. Use the lowest food rate if its slot is empty.
        return this == CULINARY ? Math.max(1, nutrition(stack)) * basePower : basePower;
    }

    ItemStack remainder(ItemStack stack) {
        return this == POTION ? new ItemStack(Items.GLASS_BOTTLE) : stack.getCraftingRemainingItem();
    }

    private static int nutrition(ItemStack stack) {
        var food = stack.getFoodProperties(null);
        return food == null ? 0 : food.getNutrition();
    }
}
