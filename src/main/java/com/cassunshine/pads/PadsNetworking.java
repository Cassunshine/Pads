package com.cassunshine.pads;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.PlayerLookup;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

public class PadsNetworking {

	public static final Identifier TELEPORT_ID = new Identifier(PadsMod.MOD_ID, "teleport");
	public static final Identifier TELEPORT_CHARGE_SOUND_ID = new Identifier(PadsMod.MOD_ID, "teleport_charge_sound");

	public static void sendTeleportCharge(ServerPlayerEntity player){
		var packet = PacketByteBufs.create();

		//Write UUID of the player...
		packet.writeUuid(player.getUuid());

		//Send packet to all players who can see this player.
		for (ServerPlayerEntity spe : PlayerLookup.tracking(player))
			ServerPlayNetworking.send(spe, TELEPORT_CHARGE_SOUND_ID, packet);
		ServerPlayNetworking.send(player, TELEPORT_CHARGE_SOUND_ID, packet);
	}

	public static void sendTeleportPacket(ServerPlayerEntity player){
		var packet = PacketByteBufs.create();

		//Write UUID of the player...
		packet.writeUuid(player.getUuid());

		//Send packet to all players who can see this player.
		for (ServerPlayerEntity spe : PlayerLookup.tracking(player))
			ServerPlayNetworking.send(spe, TELEPORT_ID, packet);
		ServerPlayNetworking.send(player, TELEPORT_ID, packet);
	}

}
