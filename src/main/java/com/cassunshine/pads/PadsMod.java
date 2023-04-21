package com.cassunshine.pads;

import com.cassunshine.pads.accessors.IPadUser;
import com.cassunshine.pads.block.PadsBlocks;
import com.cassunshine.pads.multiblock.PadsMultiblocks;
import com.cassunshine.pads.world.SpiritWorld;
import com.cassunshine.pads.world.SpiritWorldManager;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.event.ServerTickEvents;
import oshi.util.tuples.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class PadsMod implements ModInitializer {

	public static final String MOD_ID = "pads";

	public List<Pair<BlockPos, Function<Block, Boolean>>> _padCheck;

	public static Queue<Runnable> runAtEndOfTick = new LinkedList<>();

	@Override
	public void onInitialize(ModContainer mod) {

		ServerTickEvents.END.register((server) -> {
			while (runAtEndOfTick.size() > 0) {
				var next = runAtEndOfTick.poll();
				next.run();
			}

			SpiritWorldManager.tick();
		});

		PadsBlocks.initialize();
	}

	public static void evalPlayer(ServerPlayerEntity entity) {
		var spiritWorld = SpiritWorldManager.inWorlds.get(entity.getUuid());

		if (spiritWorld != null) {
			evalSpiritWorldPlayer(spiritWorld, entity);
		} else {
			var world = entity.getWorld();

			if (world == entity.getServer().getOverworld())
				evalOverworldPlayer(world, entity);
			else
				evalInvalidWorldPlayer(entity);
		}
	}

	private static void evalInvalidWorldPlayer(ServerPlayerEntity entity) {
		IPadUser user = (IPadUser) entity;

		user.setPadTicks(0);
	}

	private static void evalOverworldPlayer(ServerWorld overworld, ServerPlayerEntity entity) {
		IPadUser user = (IPadUser) entity;

		//Verify sneaking
		if (entity.isSneaking()) {
			user.setPadTicks(-40);
			return;
		}

		//Verify telepad multiblock
		var feetPos = entity.getBlockPos().add(0, -1, 0);
		if (!PadsMultiblocks.padStructure.verify(overworld, feetPos)) {
			user.setPadTicks(-40);
			return;
		}

		//Player IS sneaking and IS on telepad.
		var ticks = user.getPadTicks() + 1;
		user.setPadTicks(ticks);

		//Send charge sfx
		if (ticks == 1) PadsNetworking.sendTeleportCharge(entity);

		//Teleport player!
		if (ticks >= 35) {
			PadsNetworking.sendTeleportPacket(entity);

			var spiritWorld = SpiritWorldManager.getSpiritWorld(entity);
			//Discover this telepad, if not already discovered...
			spiritWorld.discoverPad(feetPos);

			//Take the player out of the overworld!
			spiritWorld.takePlayer(entity);

			user.setPadTicks(-9999);
		}
	}

	private static void evalSpiritWorldPlayer(SpiritWorld world, ServerPlayerEntity entity) {
		IPadUser user = (IPadUser) entity;

		if(entity.getPos().y < -30)
			entity.teleport(entity.getX(), world.runtimeWorld.getTopY(), entity.getZ());

		//Verify sneaking
		if (entity.isSneaking()) {
			user.setPadTicks(-40);
			return;
		}

		//Verify telepad multiblock
		var feetPos = entity.getBlockPos().add(0, -1, 0);
		if (!PadsMultiblocks.padStructure.verify(world.runtimeWorld, feetPos)) {
			user.setPadTicks(-40);
			return;
		}

		//Player IS sneaking and IS on telepad.
		var ticks = user.getPadTicks() + 1;
		user.setPadTicks(ticks);

		//Send charge sfx
		if (ticks == 1) PadsNetworking.sendTeleportCharge(entity);

		//Teleport player!
		if (ticks >= 35) {
			PadsNetworking.sendTeleportPacket(entity);
			var spiritWorld = SpiritWorldManager.getSpiritWorld(entity);

			//Take the player out of the overworld!
			spiritWorld.removePlayer(entity);

			user.setPadTicks(-9999);
		}
	}
}
