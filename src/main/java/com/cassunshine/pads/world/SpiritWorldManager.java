package com.cassunshine.pads.world;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpiritWorldManager {

	//Stores what spirit worlds belong to what entities.
	public static final Map<UUID, SpiritWorld> allSpiritWorlds = new HashMap<>();

	//Stores which entities are in what spirit worlds.
	private static final Map<UUID, EntityData> entitiesInWorlds = new HashMap<>();

	public static void initialize(MinecraftServer server) {
		allSpiritWorlds.clear();
		entitiesInWorlds.clear();

		try {
			var path = server.getSavePath(WorldSavePath.ROOT).resolve("pads_spirit_world.nbt");
			var component = NbtIo.read(path.toFile());
			fromNbt(server, component);
		} catch (Exception e) {
			//Ignore
		}
	}

	public static void onExit(MinecraftServer server) {
		try {
			var rootCompound = new NbtCompound();
			//Write to NBT data.
			toNbt(rootCompound);

			var path = server.getSavePath(WorldSavePath.ROOT).resolve("pads_spirit_world.nbt");
			NbtIo.write(rootCompound, path.toFile());
		} catch (Exception e) {
			//Ignore
		}

		allSpiritWorlds.clear();
		entitiesInWorlds.clear();
	}

	public static SpiritWorld getSpiritWorld(Entity entity) {
		var world = allSpiritWorlds.get(entity.getUuid());

		if (world == null) {
			world = new SpiritWorld(entity);
			allSpiritWorlds.put(entity.getUuid(), world);
		}

		return world;
	}

	public static void toNbt(NbtCompound target) {
		var spiritWorlds = new NbtCompound();

		//Write all spirit world data for players...
		for (Map.Entry<UUID, SpiritWorld> entry : allSpiritWorlds.entrySet()) {
			var worldCompound = new NbtCompound();
			entry.getValue().toNbt(worldCompound);

			spiritWorlds.put(entry.getKey().toString(), worldCompound);
		}

		target.put("worlds", spiritWorlds);


		var entities = new NbtCompound();

		for (Map.Entry<UUID, EntityData> entry : entitiesInWorlds.entrySet()) {
			var data = entry.getValue();
			var compound = new NbtCompound();
			data.toNbt(compound);
			entities.put(entry.getKey().toString(), compound);
		}

		target.put("entities", entities);
	}

	public static void fromNbt(MinecraftServer server, NbtCompound source) {
		var spiritWorlds = source.getCompound("worlds");

		for (String key : spiritWorlds.getKeys()) {
			var uuid = UUID.fromString(key);
			var compound = spiritWorlds.getCompound(key);

			var newWorld = new SpiritWorld(server, uuid, compound);

			allSpiritWorlds.put(uuid, newWorld);
		}

		var entities = source.getCompound("entities");

		for (String key : entities.getKeys()) {
			var id = UUID.fromString(key);
			var compound = entities.getCompound(key);
			var newData = new EntityData();

			newData.fromNbt(id, compound);

			entitiesInWorlds.put(id, newData);
		}
	}

	public static SpiritWorld getVisitingSpiritWorld(Entity entity) {
		var data = entitiesInWorlds.get(entity.getUuid());
		return data == null ? null : data.currentSpiritWorld;
	}

	public static void moveToSpiritWorld(ServerPlayerEntity entity, SpiritWorld world) {
		var data = new EntityData();
		data.entityId = entity.getUuid();
		data.currentSpiritWorld = world;
		data.previousMode = entity.interactionManager.getGameMode();
		data.prevInvul = entity.isInvulnerable();

		entity.setInvulnerable(true);
		entity.changeGameMode(GameMode.ADVENTURE);

		entitiesInWorlds.put(entity.getUuid(), data);

		//TODO - Block Pos Fix
		world.useInTelepad(entity, entity.getBlockPos().add(0, -1, 0));
	}

	public static void leaveSpiritWorld(ServerPlayerEntity entity) {
		var data = entitiesInWorlds.remove(entity.getUuid());
		if (data == null)
			return;

		entity.setInvulnerable(data.prevInvul);
		entity.changeGameMode(data.previousMode);

		data.currentSpiritWorld.useOutTelepad(entity, entity.getBlockPos().add(0, -1, 0));
	}


	public static class EntityData {
		public UUID entityId;
		public SpiritWorld currentSpiritWorld;

		public GameMode previousMode;
		public boolean prevInvul;

		public void toNbt(NbtCompound target) {
			target.putString("world", currentSpiritWorld.ownerID.toString());
			target.putString("mode", previousMode.asString());
			target.putBoolean("inv", prevInvul);
		}

		public void fromNbt(UUID id, NbtCompound source) {
			entityId = id;

			currentSpiritWorld = allSpiritWorlds.get(UUID.fromString(source.getString("world")));
			previousMode = GameMode.byName(source.getString("mode"));
			prevInvul = source.getBoolean("inv");
		}
	}
}
