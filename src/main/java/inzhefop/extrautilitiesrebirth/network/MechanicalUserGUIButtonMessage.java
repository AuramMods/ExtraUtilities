
package inzhefop.extrautilitiesrebirth.network;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;

import inzhefop.extrautilitiesrebirth.world.inventory.MechanicalUserGUIMenu;
import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MechanicalUserGUIButtonMessage {
	private final int buttonID, x, y, z;

	public MechanicalUserGUIButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public MechanicalUserGUIButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(MechanicalUserGUIButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(MechanicalUserGUIButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleButtonAction(entity, buttonID, x, y, z);
		});
		context.setPacketHandled(true);
	}

	private static void handleButtonAction(Player player, int buttonId, int x, int y, int z) {
		if (player != null && player.containerMenu instanceof MechanicalUserGUIMenu menu
				&& menu.position.equals(new BlockPos(x, y, z))) {
			menu.clickMenuButton(player, buttonId);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		ExtrautilitiesrebirthMod.addNetworkMessage(MechanicalUserGUIButtonMessage.class, MechanicalUserGUIButtonMessage::buffer,
				MechanicalUserGUIButtonMessage::new, MechanicalUserGUIButtonMessage::handler);
	}
}
