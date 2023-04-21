package com.cassunshine.pads.world;

import com.cassunshine.pads.PadsMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class SpiritWorld {

	public final ArrayList<ServerPlayerEntity> players = new ArrayList<>();
	public MinecraftServer server;
	public ServerWorld runtimeWorld;

	private RuntimeWorldHandle worldHandle;

	/**
	 * The position of the teleporter pad that was discovered first before anything else
	 */
	public TelepadData firstPad = null;

	public final HashMap<BlockPos, TelepadData> overworldMap = new HashMap<>();
	public final HashMap<BlockPos, TelepadData> spiritMap = new HashMap<>();

	public final Queue<Runnable> actionQueue = new LinkedList<>();

	public void takePlayer(ServerPlayerEntity entity) {
		PadsMod.runAtEndOfTick.add(() -> {
			//Track player in world, so we know when to delete world.
			if (players.size() == 0)
				createWorld(entity.server, entity.getBlockPos());
			players.add(entity);
			SpiritWorldManager.inWorlds.put(entity.getUuid(), this);

			var feetPos = entity.getBlockPos().add(0, -1, 0);
			var pad = overworldMap.get(feetPos);

			if (pad == null)
				throw new RuntimeException("Telepad not found to match overworld position " + feetPos);

			var padPos = pad.spiritWorldPos;
			entity.teleport(runtimeWorld, padPos.getX() + 0.5f, padPos.getY() + 1.75, padPos.getZ() + 0.5f, entity.getYaw(), entity.getPitch());
		});
	}

	public void removePlayer(ServerPlayerEntity entity) {
		PadsMod.runAtEndOfTick.add(() -> {
			players.remove(entity);
			if (players.size() == 0)
				deleteWorld();
			SpiritWorldManager.inWorlds.remove(entity.getUuid());

			var feetPos = entity.getBlockPos().add(0, -1, 0);
			var pad = spiritMap.get(feetPos);

			if (pad == null)
				throw new RuntimeException("Telepad not found to match spirit world position " + feetPos);

			var padPos = pad.overworldPos;
			entity.teleport(entity.getServer().getOverworld(), padPos.getX() + 0.5f, padPos.getY() + 1.75, padPos.getZ() + 0.5f, entity.getYaw(), entity.getPitch());
		});
	}


	private void createWorld(MinecraftServer server, BlockPos overworldPos) {
		var fantasy = Fantasy.get(server);

		var dims = server.getRegistryManager().get(RegistryKeys.DIMENSION);
		var type = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).get(new Identifier("pads:spirit_world"));

		var config = new RuntimeWorldConfig()
			.setDifficulty(Difficulty.NORMAL)
			.setDimensionType(server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getKey(type).get())
			.setGameRule(GameRules.DO_DAYLIGHT_CYCLE, false)
			.setGameRule(GameRules.DO_MOB_SPAWNING, false)
			.setGameRule(GameRules.DO_IMMEDIATE_RESPAWN, true)
			.setGameRule(GameRules.DO_MOB_GRIEFING, false)
			.setGameRule(GameRules.DO_VINES_SPREAD, false)
			.setGenerator(dims.get(new Identifier(PadsMod.MOD_ID, "spirit_world")).getChunkGenerator())
			.setSeed(0);

		worldHandle = fantasy.openTemporaryWorld(config);
		runtimeWorld = worldHandle.asWorld();

		SpiritWorldBuilder.buildWorld(server.getOverworld(), this);

		var list = new ArrayList<>(spiritMap.values());
		list.sort((a, b) -> (int) (a.overworldPos.getSquaredDistance(overworldPos) - b.overworldPos.getSquaredDistance(overworldPos)));

		for (TelepadData telepadData : list) {
			actionQueue.add(() -> SpiritWorldBuilder.copyTelepad(server.getOverworld(), this.runtimeWorld, telepadData.overworldPos, telepadData.spiritWorldPos));
		}
	}

	private void deleteWorld() {
		server = null;
		runtimeWorld = null;
		worldHandle.delete();
		worldHandle = null;
	}

	/**
	 * Discovers a teleporter pad in the overworld at the given position.
	 */
	public void discoverPad(BlockPos pos) {
		//TODO - Verify teleporter pad...
		if (overworldMap.containsKey(pos))
			return;

		var newPad = new TelepadData();
		newPad.overworldPos = pos;
		overworldMap.put(pos, newPad);


		if (firstPad == null)
			firstPad = newPad;
	}

	/**
	 * Verifies the existence of discovered teleporter pads.
	 * If any are not found, they are removed.
	 */
	public void verifyPads(World world) {

	}


	public void tick() {
		if (actionQueue.size() > 0)
			actionQueue.poll().run();
	}

	public void toNbt(NbtCompound target) {

	}

	public void fromNbt(NbtCompound source) {

	}


	public class TelepadData {
		public BlockPos overworldPos = BlockPos.ORIGIN;

		public Vec3d spiritCalcPos = Vec3d.ZERO;
		public BlockPos spiritWorldPos = BlockPos.ORIGIN;
	}
}
