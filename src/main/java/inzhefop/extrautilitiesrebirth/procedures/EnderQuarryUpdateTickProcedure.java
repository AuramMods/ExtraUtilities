package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.EnderQuarryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public class EnderQuarryUpdateTickProcedure {
	private static final int MINING = 1;
	private static final int FINISHED = 2;
	private static final int OUTPUT_BLOCKED = 3;
	private static final TagKey<Item> ORES = ItemTags.create(new ResourceLocation("forge", "ores"));
	private static final Direction[] OUTPUT_SIDES = {
			Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
	};

	public static void execute(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof EnderQuarryBlockEntity quarry)) {
			return;
		}

		CompoundTag data = quarry.getPersistentData();
		int previousState = data.getInt("state");
		boolean blocked = !flushOutput(level, quarry);
		if (previousState != MINING && previousState != OUTPUT_BLOCKED) {
			return;
		}

		int state = blocked ? OUTPUT_BLOCKED : MINING;
		double speed = data.getDouble("eqspeed");
		double ticks = data.getDouble("ticks");
		double previousTicks = ticks;
		double minedBlocks = data.getDouble("minedblocks");
		boolean advanced = false;
		IEnergyStorage energy = quarry.getCapability(ForgeCapabilities.ENERGY).orElse(null);
		if (!blocked && speed > 0 && energy != null) {
			int iterations = (int) Math.pow(2, Math.round(speed * 0.15));
			int energyCost = (int) (data.getDouble("eqpowerdrain") * iterations);
			for (int i = 0; i < iterations && energy.getEnergyStored() >= energyCost; i++) {
				if (energyCost > 0) {
					energy.extractEnergy(energyCost, false);
				}
				if (++ticks < 20 / speed) {
					continue;
				}

				BlockPos target = BlockPos.containing(data.getDouble("xcoord"), data.getDouble("ycoord"),
						data.getDouble("zcoord"));
				mineBlock(level, quarry, target, data);
				minedBlocks++;
				ticks = 0;
				advanced = true;
				boolean finished = advanceCursor(data, level.getMinBuildHeight());
				blocked = !flushOutput(level, quarry);
				if (finished) {
					state = FINISHED;
					break;
				}
				if (blocked) {
					state = OUTPUT_BLOCKED;
					break;
				}
			}
		}

		if (advanced || ticks != previousTicks || state != previousState) {
			data.putDouble("ticks", ticks);
			data.putDouble("minedblocks", minedBlocks);
			data.putDouble("state", state);
			quarry.setChanged();
			BlockState blockState = quarry.getBlockState();
			level.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_CLIENTS);
		}
	}

	private static void mineBlock(ServerLevel level, EnderQuarryBlockEntity quarry, BlockPos target, CompoundTag data) {
		BlockState state = level.getBlockState(target);
		if (target.equals(quarry.getBlockPos()) || state.isAir() || state.getDestroySpeed(level, target) < 0) {
			return;
		}

		boolean silk = data.getBoolean("eqsilk");
		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		if (silk) {
			tool.enchant(Enchantments.SILK_TOUCH, 1);
		}
		LootParams.Builder loot = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(target))
				.withParameter(LootContextParams.TOOL, tool)
				.withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(target));
		List<ItemStack> drops = state.getDrops(loot);
		if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
			return;
		}

		boolean multiplyOreDrops = !silk && state.getBlock().asItem().getDefaultInstance().is(ORES);
		for (ItemStack drop : drops) {
			if (multiplyOreDrops && !drop.is(state.getBlock().asItem())) {
				drop = drop.copyWithCount((int) (drop.getCount() * Math.max(1, data.getDouble("eqfortune"))));
			}
			if (!drop.isEmpty()) {
				quarry.queueMinedDrop(drop);
			}
		}
		if (!data.getBoolean("eqhole")) {
			level.setBlock(target, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
		}
		level.sendParticles(ParticleTypes.PORTAL, target.getX() + 0.5, target.getY() + 0.5,
				target.getZ() + 0.5, 5, 0.3, 0.3, 0, 0);
	}

	static boolean advanceCursor(CompoundTag data, int minY) {
		double y = data.getDouble("ycoord");
		if (y > minY) {
			data.putDouble("ycoord", y - 1);
			return false;
		}

		data.putDouble("ycoord", data.getDouble("areamaxy"));
		double z = data.getDouble("zcoord");
		double endZ = data.getDouble("areamaxz");
		switch (data.getInt("areadirect1")) {
			case 1 -> {
				if (z > endZ) {
					data.putDouble("zcoord", z - 1);
					return false;
				}
			}
			case 3 -> {
				if (z < endZ) {
					data.putDouble("zcoord", z + 1);
					return false;
				}
			}
			default -> {
				return true;
			}
		}

		data.putDouble("zcoord", data.getDouble("areaminz"));
		double x = data.getDouble("xcoord");
		double endX = data.getDouble("areamaxx");
		if (data.getInt("areadirect2") == 2 && x < endX) {
			data.putDouble("xcoord", x + 1);
			return false;
		}
		if (data.getInt("areadirect2") == 4 && x > endX) {
			data.putDouble("xcoord", x - 1);
			return false;
		}
		return true;
	}

	private static boolean flushOutput(ServerLevel level, EnderQuarryBlockEntity quarry) {
		quarry.refillOutputSlot();
		while (!quarry.getItem(0).isEmpty()) {
			ItemStack output = quarry.getItem(0);
			ItemStack remainder = output.copy();
			for (Direction side : OUTPUT_SIDES) {
				BlockEntity neighbor = level.getBlockEntity(quarry.getBlockPos().relative(side));
				if (neighbor != null) {
					IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite())
							.orElse(null);
					if (handler != null) {
						remainder = ItemHandlerHelper.insertItemStacked(handler, remainder, false);
						if (remainder.isEmpty()) {
							break;
						}
					}
				}
			}
			if (remainder.getCount() == output.getCount()) {
				return false;
			}
			quarry.setItem(0, remainder);
			if (!remainder.isEmpty()) {
				return false;
			}
			quarry.refillOutputSlot();
		}
		return true;
	}
}
