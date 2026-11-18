package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WirelessFEBatteryBlockEntity extends BlockEntity {
    private static final int CAPACITY = 3_300;
    private static final int MAX_CONNECTIONS = 10;
    private static final int RANGE = 10;
    private static final String CONNECTIONS_TAG = "connections";
    private static final String ENERGY_TAG = "energyStorage";

    private final List<BlockPos> links = new ArrayList<>();
    private final EnergyStorage storage = new EnergyStorage(CAPACITY) {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(Math.max(0, amount), simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(Math.max(0, amount), simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    };
    private LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> storage);

    public WirelessFEBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.WIRELESS_FE_BATTERY.get(), pos, state);
    }

    public void tickOutput(ServerLevel level) {
        if (links.isEmpty() || storage.getEnergyStored() == 0) {
            return;
        }

        List<IEnergyStorage> receivers = new ArrayList<>(links.size());
        for (BlockPos pos : links) {
            if (level.hasChunkAt(pos)) {
                IEnergyStorage receiver = findReceiver(level.getBlockEntity(pos));
                if (receiver != null) {
                    receivers.add(receiver);
                }
            }
        }

        for (int index = 0; index < receivers.size(); index++) {
            int available = storage.getEnergyStored();
            int remaining = receivers.size() - index;
            int offer = (available + remaining - 1) / remaining;
            if (offer == 0) {
                break;
            }
            int accepted = receivers.get(index).receiveEnergy(offer, false);
            if (accepted > 0) {
                storage.extractEnergy(accepted, false);
            }
        }
    }

    public static @Nullable IEnergyStorage findReceiver(@Nullable BlockEntity target) {
        if (target == null || target instanceof WirelessFEBatteryBlockEntity) {
            return null;
        }
        IEnergyStorage unrestricted = target.getCapability(ForgeCapabilities.ENERGY).filter(IEnergyStorage::canReceive).orElse(null);
        if (unrestricted != null) {
            return unrestricted;
        }
        for (Direction side : Direction.values()) {
            IEnergyStorage sided = target.getCapability(ForgeCapabilities.ENERGY, side).filter(IEnergyStorage::canReceive).orElse(null);
            if (sided != null) {
                return sided;
            }
        }
        return null;
    }

    public Component changeLink(ServerLevel level, BlockPos pos, boolean remove) {
        if (remove && links.remove(pos)) {
            writeLinks();
            return Component.translatable("message.extrautilitiesrebirth.wireless.unlinked", pos.toShortString());
        }
        if (links.contains(pos)) {
            return Component.translatable("message.extrautilitiesrebirth.wireless.already_linked");
        }
        if (!inRange(pos)) {
            return Component.translatable("message.extrautilitiesrebirth.wireless.out_of_range", RANGE);
        }
        if (links.size() == MAX_CONNECTIONS) {
            return Component.translatable("message.extrautilitiesrebirth.wireless.full", MAX_CONNECTIONS);
        }
        if (!level.hasChunkAt(pos) || findReceiver(level.getBlockEntity(pos)) == null) {
            return Component.translatable("message.extrautilitiesrebirth.wireless.missing_receiver");
        }
        links.add(pos.immutable());
        writeLinks();
        return Component.translatable("message.extrautilitiesrebirth.wireless.linked", pos.toShortString(), links.size(), MAX_CONNECTIONS);
    }

    public void showStatus(ServerLevel level, Player player) {
        player.displayClientMessage(Component.translatable("message.extrautilitiesrebirth.wireless.status",
                storage.getEnergyStored(), CAPACITY, links.size(), MAX_CONNECTIONS), false);
        for (BlockPos pos : links) {
            Component name = level.hasChunkAt(pos) ? level.getBlockState(pos).getBlock().getName()
                    : Component.translatable("message.extrautilitiesrebirth.wireless.unloaded");
            player.displayClientMessage(Component.translatable("message.extrautilitiesrebirth.wireless.receiver", name, pos.toShortString()), false);
        }
    }

    private boolean inRange(BlockPos pos) {
        return !worldPosition.equals(pos) && worldPosition.distSqr(pos) < RANGE * RANGE;
    }

    private void writeLinks() {
        CompoundTag data = getPersistentData();
        data.putDouble(CONNECTIONS_TAG, links.size());
        for (int index = 0; index < MAX_CONNECTIONS; index++) {
            String key = "connection" + (index + 1);
            if (index < links.size()) {
                BlockPos pos = links.get(index);
                data.putDouble(key + "x", pos.getX());
                data.putDouble(key + "y", pos.getY());
                data.putDouble(key + "z", pos.getZ());
            } else {
                data.remove(key + "x");
                data.remove(key + "y");
                data.remove(key + "z");
            }
        }
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storage.deserializeNBT(IntTag.valueOf(Mth.clamp(tag.getInt(ENERGY_TAG), 0, CAPACITY)));

        links.clear();
        CompoundTag data = getPersistentData();
        int count = Mth.clamp(data.getInt(CONNECTIONS_TAG), 0, MAX_CONNECTIONS);
        for (int index = 1; index <= count; index++) {
            String key = "connection" + index;
            if (data.contains(key + "x", Tag.TAG_ANY_NUMERIC) && data.contains(key + "y", Tag.TAG_ANY_NUMERIC)
                    && data.contains(key + "z", Tag.TAG_ANY_NUMERIC)) {
                BlockPos pos = new BlockPos(data.getInt(key + "x"), data.getInt(key + "y"), data.getInt(key + "z"));
                if (inRange(pos) && !links.contains(pos)) {
                    links.add(pos);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ENERGY_TAG, storage.serializeNBT());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.ENERGY) {
            return energy.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energy = LazyOptional.of(() -> storage);
    }
}
