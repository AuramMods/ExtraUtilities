package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.IItemHandler;

import net.minecraftforge.energy.EnergyStorage;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;


import inzhefop.extrautilitiesrebirth.world.inventory.EnderQuarryGUIMenu;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;

public class QuantumQuarryBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	public static final int UPGRADE_SLOT = 3;
	public static final ResourceKey<Level> MINING_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			new ResourceLocation(ExtrautilitiesrebirthMod.MODID, "quantum_quarry"));
	private static final TicketType<ChunkPos> MINING_TICKET = TicketType.create(
			"extrautilitiesrebirth:quantum_quarry", Comparator.comparingLong(ChunkPos::toLong), 3);
	private static final int ENERGY_PER_BLOCK = 20_000;
	private static final int[] OUTPUT_SLOTS = {0, 1, 2};
	private final Deque<ItemStack> pendingDrops = new ArrayDeque<>();
	private long miningSeed;
	private boolean hasMiningSeed;
	private long miningCursor;
	private long minedBlocks;
	private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(4, ItemStack.EMPTY);
	private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());

	public QuantumQuarryBlockEntity(BlockPos position, BlockState state) {
		super(ExtrautilitiesrebirthModBlockEntities.QUANTUM_QUARRY.get(), position, state);
	}
 
	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks);
		if (compound.get("energyStorage") instanceof IntTag intTag)
			energyStorage.deserializeNBT(intTag);
		hasMiningSeed = compound.contains("MiningSeed", Tag.TAG_LONG);
		miningSeed = compound.getLong("MiningSeed");
		miningCursor = Math.max(0, compound.getLong("MiningCursor"));
		minedBlocks = compound.getLong("MinedBlocks");
		pendingDrops.clear();
		ListTag savedDrops = compound.getList("PendingDrops", Tag.TAG_COMPOUND);
		for (int i = 0; i < savedDrops.size(); i++) {
			ItemStack stack = ItemStack.of(savedDrops.getCompound(i));
			if (!stack.isEmpty()) {
				pendingDrops.addLast(stack);
			}
		}
	}

	@Override
	public void saveAdditional(CompoundTag compound) {
		super.saveAdditional(compound);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks);
		}
		compound.put("energyStorage", energyStorage.serializeNBT());
		if (hasMiningSeed) {
			compound.putLong("MiningSeed", miningSeed);
			compound.putLong("MiningCursor", miningCursor);
			compound.putLong("MinedBlocks", minedBlocks);
		}
		if (!pendingDrops.isEmpty()) {
			ListTag savedDrops = new ListTag();
			for (ItemStack stack : pendingDrops) {
				savedDrops.add(stack.save(new CompoundTag()));
			}
			compound.put("PendingDrops", savedDrops);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return this.saveWithFullMetadata();
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("quantum_quarry");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new EnderQuarryGUIMenu(id, inventory, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Quantum Quarry");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return index == UPGRADE_SLOT && isMiningBook(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return OUTPUT_SLOTS.clone();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return index >= 0 && index < UPGRADE_SLOT;
	}

	public boolean isAssembled() {
		if (level == null) {
			return false;
		}
		for (Direction direction : Direction.values()) {
			if (!level.getBlockState(worldPosition.relative(direction)).is(ExtrautilitiesrebirthModBlocks.QUANTUM_QUARRY_ACTUATOR.get())) {
				return false;
			}
		}
		return true;
	}

	public void tick(ServerLevel world) {
		if (!isAssembled()) {
			return;
		}

		exportOutput(world);
		if (!acceptPendingDrops() || energyStorage.getEnergyStored() < ENERGY_PER_BLOCK) {
			return;
		}
		ServerLevel miningWorld = world.getServer().getLevel(MINING_DIMENSION);
		if (miningWorld == null) {
			return;
		}
		if (!hasMiningSeed) {
			miningSeed = world.random.nextLong();
			hasMiningSeed = true;
			setChanged();
		}

		// Request one quarry chunk without waiting on world generation on the server thread.
		int chunkVolume = 256 * miningWorld.getHeight();
		long chunkIndex = miningCursor / chunkVolume;
		ChunkPos chunkPos = miningChunkPos(chunkIndex);
		var chunks = miningWorld.getChunkSource();
		chunks.addRegionTicket(MINING_TICKET, chunkPos, 0, chunkPos);
		LevelChunk chunk = chunks.getChunkNow(chunkPos.x, chunkPos.z);
		if (chunk == null) {
			return;
		}

		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		ItemStack book = getItem(UPGRADE_SLOT);
		if (isMiningBook(book)) {
			EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(book), tool);
		}
		int operations = 1 + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, tool);
		for (int i = 0; i < operations && energyStorage.getEnergyStored() >= ENERGY_PER_BLOCK; i++) {
			if (!mineNext(miningWorld, chunk, chunkVolume, chunkIndex, tool)) {
				break;
			}
			if (!acceptPendingDrops()) {
				break;
			}
		}
	}

	private ChunkPos miningChunkPos(long index) {
		int originX = (int) (miningSeed & 2047) - 1024;
		int originZ = (int) ((miningSeed >>> 11) & 2047) - 1024;
		return new ChunkPos(originX + (int) (index & 1023), originZ + (int) ((index >>> 10) & 1023));
	}

	private boolean mineNext(ServerLevel miningWorld, LevelChunk chunk, int volume, long chunkIndex, ItemStack tool) {
		int height = miningWorld.getHeight();
		for (int scanned = 0; scanned < 32 && miningCursor / volume == chunkIndex; scanned++) {
			int offset = (int) (miningCursor % volume);
			int column = offset / height;
			int localX = column & 15;
			int localZ = column >>> 4;
			int y = miningWorld.getMaxBuildHeight() - 1 - offset % height;
			int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
			if (y > surface) {
				int skip = Math.min(y - surface, y - miningWorld.getMinBuildHeight() + 1);
				miningCursor += skip;
				setChanged();
				continue;
			}

			BlockPos target = chunk.getPos().getBlockAt(localX, y, localZ);
			BlockState state = chunk.getBlockState(target);
			miningCursor++;
			setChanged();
			if (state.isAir() || state.liquid() || state.hasBlockEntity()
					|| state.getDestroySpeed(miningWorld, target) < 0
					|| state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
				continue;
			}

			// Sample a reserved mining region rather than removing blocks in the player's world.
			var loot = new LootParams.Builder(miningWorld)
					.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(target))
					.withParameter(LootContextParams.TOOL, tool);
			for (ItemStack drop : state.getDrops(loot)) {
				while (!drop.isEmpty()) {
					pendingDrops.addLast(drop.split(Math.min(drop.getMaxStackSize(), getMaxStackSize())));
				}
			}
			minedBlocks++;
			energyStorage.extractEnergy(ENERGY_PER_BLOCK, false);
			return true;
		}
		return false;
	}

	private void exportOutput(ServerLevel world) {
		for (int slot : OUTPUT_SLOTS) {
			ItemStack buffered = getItem(slot);
			if (buffered.isEmpty()) {
				continue;
			}
			ItemStack remainder = buffered.copy();
			for (Direction arm : Direction.values()) {
				BlockPos actuator = worldPosition.relative(arm);
				for (Direction outward : Direction.values()) {
					if (outward == arm.getOpposite()) {
						continue;
					}
					BlockPos target = actuator.relative(outward);
					BlockEntity neighbor = world.hasChunkAt(target) ? world.getBlockEntity(target) : null;
					if (neighbor != null) {
						IItemHandler receiver = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, outward.getOpposite())
								.orElse(null);
						if (receiver != null) {
							remainder = ItemHandlerHelper.insertItemStacked(receiver, remainder, false);
							if (remainder.isEmpty()) {
								break;
							}
						}
					}
				}
				if (remainder.isEmpty()) {
					break;
				}
			}
			if (remainder.getCount() != buffered.getCount()) {
				setItem(slot, remainder);
			}
		}
	}

	private boolean acceptPendingDrops() {
		while (!pendingDrops.isEmpty()) {
			ItemStack drop = pendingDrops.peekFirst();
			for (int slot : OUTPUT_SLOTS) {
				ItemStack current = getItem(slot);
				int limit = Math.min(drop.getMaxStackSize(), getMaxStackSize());
				if (current.isEmpty() || ItemHandlerHelper.canItemStacksStack(current, drop)) {
					int accepted = Math.min(limit - current.getCount(), drop.getCount());
					if (accepted > 0) {
						ItemStack merged = drop.copyWithCount(current.getCount() + accepted);
						drop.shrink(accepted);
						setItem(slot, merged);
						if (drop.isEmpty()) {
							break;
						}
					}
				}
			}
			if (!drop.isEmpty()) {
				return false;
			}
			pendingDrops.removeFirst();
		}
		return true;
	}

	public void dropPendingDrops() {
		if (level != null && !level.isClientSide) {
			while (!pendingDrops.isEmpty()) {
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
						worldPosition.getZ(), pendingDrops.removeFirst());
			}
		}
	}

	public static boolean isMiningBook(ItemStack stack) {
		return stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantments(stack).keySet().stream()
				.anyMatch(enchantment -> enchantment == Enchantments.BLOCK_EFFICIENCY
						|| enchantment == Enchantments.BLOCK_FORTUNE || enchantment == Enchantments.SILK_TOUCH);
	}

	private final EnergyStorage energyStorage = new EnergyStorage(10000000, 10000000, 10000000, 0) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int retval = super.receiveEnergy(maxReceive, simulate);
			if (!simulate && retval > 0 && level != null) {
				setChanged();
				level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return retval;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			int retval = super.extractEnergy(maxExtract, simulate);
			if (!simulate && retval > 0 && level != null) {
				setChanged();
				level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return retval;
		}
	};

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
		if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
			return handlers[facing.ordinal()].cast();
		if (!this.remove && capability == ForgeCapabilities.ENERGY)
			return LazyOptional.of(() -> energyStorage).cast();
		return super.getCapability(capability, facing);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		for (LazyOptional<? extends IItemHandler> handler : handlers)
			handler.invalidate();
	}
}
