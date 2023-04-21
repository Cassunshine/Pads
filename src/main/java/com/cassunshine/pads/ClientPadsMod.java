package com.cassunshine.pads;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

public class ClientPadsMod implements ClientModInitializer {


	@Override
	public void onInitializeClient(ModContainer mod) {
		ClientPlayNetworking.registerGlobalReceiver(PadsNetworking.TELEPORT_ID, this::handleTeleportPacket);
		ClientPlayNetworking.registerGlobalReceiver(PadsNetworking.TELEPORT_CHARGE_SOUND_ID, this::handleChargeSoundPacket);
	}

	private void handleTeleportPacket(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
		if (client.world == null)
			return;

		var pos = packetByteBuf.readBlockPos().add(0, 1, 0);
		client.world.playSound(pos, PadsSounds.TELEPORT_SOUND_EVENT, SoundCategory.PLAYERS, 0.3f, 1f, false);
	}

	private void handleChargeSoundPacket(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
		if (client.world == null)
			return;

		var pos = packetByteBuf.readBlockPos().add(0, 1, 0);
		client.world.playSound(pos, PadsSounds.TELEPORT_CHARGE_SOUND_EVENT, SoundCategory.PLAYERS, 0.3f, 1f, false);
	}
}
