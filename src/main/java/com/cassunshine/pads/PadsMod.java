package com.cassunshine.pads;

import com.cassunshine.pads.accessors.IPadUser;
import com.cassunshine.pads.block.PadsBlocks;
import com.cassunshine.pads.multiblock.PadsMultiblocks;
import com.cassunshine.pads.multiblock.TelepadMultiblockStructure;
import com.cassunshine.pads.world.SpiritWorld;
import com.cassunshine.pads.world.SpiritWorldManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.lifecycle.api.event.ServerTickEvents;

import java.util.LinkedList;
import java.util.Queue;

public class PadsMod implements ModInitializer {

	public static final String MOD_ID = "pads";

	public static Queue<Runnable> runAtEndOfTick = new LinkedList<>();

	@Override
	public void onInitialize(ModContainer mod) {

		ServerLifecycleEvents.STARTING.register((s) -> runAtEndOfTick.add(() -> {
			SpiritWorldManager.initialize(s);
		}));

		ServerLifecycleEvents.STOPPED.register(SpiritWorldManager::onExit);

		ServerTickEvents.END.register((server) -> {
			while (runAtEndOfTick.size() > 0) {
				var next = runAtEndOfTick.poll();
				next.run();
			}
		});


		PadsBlocks.initialize();
	}

	public static void evalPlayer(ServerPlayerEntity entity) {

		if (entity.getWorld() == entity.getServer().getOverworld()) {
			evalOverworldPlayer(entity.getWorld(), entity);
		} else {
			var spiritWorld = SpiritWorldManager.getVisitingSpiritWorld(entity);
			if (spiritWorld != null)
				evalSpiritWorldPlayer(spiritWorld, entity);
			else
				evalInvalidWorldPlayer(entity);
		}
	}

	private static void evalInvalidWorldPlayer(ServerPlayerEntity entity) {
		IPadUser user = (IPadUser) entity;

		user.setPadTicks(-40);
	}

	private static void evalOverworldPlayer(ServerWorld overworld, ServerPlayerEntity entity) {
		if (!useTelepad(PadsMultiblocks.padStructure, overworld, entity)) return;

		PadsMod.runAtEndOfTick.add(() -> SpiritWorldManager.moveToSpiritWorld(entity, SpiritWorldManager.getSpiritWorld(entity)));
	}

	private static void evalSpiritWorldPlayer(SpiritWorld world, ServerPlayerEntity entity) {
		if (entity.getPos().y < -30) entity.teleport(entity.getX(), world.actualWorld.getTopY(), entity.getZ());

		if (!useTelepad(PadsMultiblocks.padStructureWithStone, world.actualWorld, entity)) return;

		PadsMod.runAtEndOfTick.add(() -> SpiritWorldManager.leaveSpiritWorld(entity));
	}


	private static boolean useTelepad(TelepadMultiblockStructure structure, World world, ServerPlayerEntity entity) {

		IPadUser user = (IPadUser) entity;

		//Verify sneaking
		if (!entity.isSneaking()) {
			user.setPadTicks(-40);
			return false;
		}

		//Verify telepad multiblock
		var feetPos = entity.getBlockPos().add(0, -1, 0);
		if (!structure.verify(world, feetPos)) {
			user.setPadTicks(-40);
			return false;
		}

		//Player IS sneaking and IS on telepad.
		var ticks = user.getPadTicks() + 1;
		user.setPadTicks(ticks);

		//Send charge sfx
		if (ticks == 1) PadsNetworking.sendTeleportCharge(entity);

		//Teleport player!
		if (ticks >= 35) {
			PadsNetworking.sendTeleportPacket(entity);
			user.setPadTicks(-9999);
			return true;
		}

		return false;
	}
}
