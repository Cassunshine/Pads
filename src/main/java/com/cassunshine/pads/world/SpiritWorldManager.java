package com.cassunshine.pads.world;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;

import java.util.*;

public class SpiritWorldManager {

	//Stores the spirit worlds for each player.
	public static final Map<UUID, SpiritWorld> allSpiritWorlds = new HashMap<>();

	//Stores which players are in what spirit worlds.
	public static final Map<UUID, SpiritWorld> inWorlds = new HashMap<>();

	public static void initialize() {


	}

	public static SpiritWorld getSpiritWorld(Entity entity) {
		var world = allSpiritWorlds.get(entity.getUuid());

		if (world == null) {
			world = new SpiritWorld();
			allSpiritWorlds.put(entity.getUuid(), world);
		}

		//Verify teleporter pads existence.
		world.verifyPads(entity.getServer().getOverworld());

		return world;
	}

	public static void tick(){
		for (SpiritWorld value : allSpiritWorlds.values())
			value.tick();
	}

	public static void toNbt(NbtCompound target) {

	}

	public static void fromNbt(NbtCompound source) {

	}
}
