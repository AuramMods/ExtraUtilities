package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public class DemonIngotItem extends Item {
    private static final Map<ServerLevel, Set<ItemEntity>> GOLD_IN_LEVEL = new WeakHashMap<>();
    private static final Set<Block> POOL_WALLS = Set.of(Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICK_FENCE);

    public DemonIngotItem() {
        super(new Item.Properties().fireResistant());
    }

    @SubscribeEvent
    public static void trackGold(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ItemEntity item && item.getItem().is(Items.GOLD_INGOT)) {
            GOLD_IN_LEVEL.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new WeakHashMap<>())).add(item);
        }
    }

    @SubscribeEvent
    public static void convertGold(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.level instanceof ServerLevel level)) {
            return;
        }
        var gold = GOLD_IN_LEVEL.get(level);
        if (gold != null) {
            gold.removeIf(item -> convertOrStopTracking(level, item));
        }
    }

    private static boolean convertOrStopTracking(ServerLevel level, ItemEntity item) {
        if (!item.isAlive() || !item.getItem().is(Items.GOLD_INGOT)) {
            return true;
        }
        // Check contact before the item tick applies lava damage.
        var bounds = item.getBoundingBox().inflate(0.1);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            var fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.LAVA) || bounds.minY >= pos.getY() + fluid.getHeight(level, pos)) {
                continue;
            }
            boolean enclosed = true;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!POOL_WALLS.contains(level.getBlockState(pos.relative(side)).getBlock())) {
                    enclosed = false;
                    break;
                }
            }
            if (enclosed) {
                item.setItem(new ItemStack(ExtrautilitiesrebirthModItems.DEMON_INGOT.get(), item.getItem().getCount()));
                item.clearFire();
                return true;
            }
        }
        return false;
    }
}
