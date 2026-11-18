
package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.ArrayDeque;
import java.util.HashSet;


public class FireAxeItem extends AxeItem {
	public FireAxeItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 14f;
			}

			public float getAttackDamageBonus() {
				return 6f;
			}

			public int getLevel() {
				return 4;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(Items.NETHERITE_AXE));
			}
		}, 1, 1f, new Item.Properties().fireResistant());
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState original, BlockPos pos, LivingEntity entity) {
		boolean result = super.mineBlock(stack, level, original, pos, entity);
		if (level instanceof ServerLevel serverLevel && entity instanceof Player player && original.is(BlockTags.LOGS)
				&& !level.getBlockState(pos).is(original.getBlock())) {
			fellConnectedLogs(serverLevel, pos, player, stack);
		}
		return result;
	}

	private static void fellConnectedLogs(ServerLevel level, BlockPos origin, Player player, ItemStack stack) {
		var pending = new ArrayDeque<BlockPos>();
		var checked = new HashSet<BlockPos>();
		pending.add(origin.immutable());
		checked.add(origin.immutable());
		int harvested = 0;
		while (!pending.isEmpty()) {
			var log = pending.removeFirst();
			for (BlockPos cursor : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 1, 1))) {
				var next = cursor.immutable();
				if (!checked.add(next) || !level.hasChunkAt(next)) {
					continue;
				}
				var state = level.getBlockState(next);
				if (!state.is(BlockTags.LOGS)) {
					continue;
				}
				var blockEntity = level.getBlockEntity(next);
				if (ToolHarvest.breakBlock(level, next, state, player, Direction.UP, stack)) {
					Block.dropResources(state, level, next, blockEntity, player, stack);
					pending.add(next);
					// Bound one mining action even if it reaches a large player-built structure.
					if (++harvested >= 512) {
						return;
					}
				}
			}
		}
	}
}
