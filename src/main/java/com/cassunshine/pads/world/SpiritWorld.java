package com.cassunshine.pads.world;

import com.cassunshine.pads.PadsMod;
import com.cassunshine.pads.multiblock.PadsMultiblocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.*;

public class SpiritWorld {

	public final UUID ownerID;
	public final MinecraftServer server;

	public final RuntimeWorldHandle worldHandle;
	public final ServerWorld actualWorld;


	public final ArrayList<TelepadData> allDiscoveredPads = new ArrayList<>();


	/**
	 * Maps overworld positions to their appropriate teleporter pad data.
	 */
	public final HashMap<BlockPos, TelepadData> overworldMap = new HashMap<>();

	/**
	 * Maps spirit world positions to their appropriate teleporter pad data.
	 */
	public final HashMap<BlockPos, TelepadData> spiritWorldMap = new HashMap<>();

	/**
	 * Records which chunks have been modified by the spirit world builder.
	 */
	public final HashSet<BlockPos> modifiedChunkPositions = new HashSet<>();


	public SpiritWorld(@NotNull Entity ownerEntity) {
		this(ownerEntity.getServer(), ownerEntity.getUuid(), null);
	}

	public SpiritWorld(MinecraftServer server, UUID ownerID, NbtCompound compound) {
		this.ownerID = ownerID;
		this.server = server;

		if (compound != null)
			loadFromNBT(compound);

		worldHandle = setupWorld();
		actualWorld = worldHandle.asWorld();
	}

	/**
	 * Sets up the actual in-game world to be used for this Spirit World.
	 *
	 * @return The RuntimeWorldHandle that's associated with this spirit world
	 */
	private RuntimeWorldHandle setupWorld() {
		var fantasy = Fantasy.get(server);

		var dims = server.getRegistryManager().get(RegistryKeys.DIMENSION);
		var type = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).get(new Identifier("pads:spirit_world"));

		var config = new RuntimeWorldConfig()
			.setDifficulty(Difficulty.PEACEFUL)
			.setDimensionType(server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getKey(type).get())
			.setGameRule(GameRules.DO_DAYLIGHT_CYCLE, false)
			.setGameRule(GameRules.DO_MOB_SPAWNING, false)
			.setGameRule(GameRules.DO_IMMEDIATE_RESPAWN, true)
			.setGameRule(GameRules.DO_MOB_GRIEFING, false)
			.setGameRule(GameRules.DO_VINES_SPREAD, false)
			.setGenerator(dims.get(new Identifier(PadsMod.MOD_ID, "spirit_world")).getChunkGenerator())
			.setSeed(0);

		return fantasy.getOrOpenPersistentWorld(new Identifier(PadsMod.MOD_ID, "sw_" + ownerID.toString()), config);
	}

	public void toNbt(NbtCompound target) {
		var discovered = new NbtList();
		var modChunks = new NbtList();


		for (TelepadData pad : allDiscoveredPads) {
			var compound = new NbtCompound();
			compound.put("ow", NbtHelper.fromBlockPos(pad.overworldPosition));
			compound.put("sw", NbtHelper.fromBlockPos(pad.spiritWorldPosition));

			discovered.add(compound);
		}


		for (BlockPos pos : modifiedChunkPositions)
			modChunks.add(NbtHelper.fromBlockPos(pos));

		target.put("pads", discovered);
		target.put("chunks", modChunks);
	}

	private void loadFromNBT(NbtCompound compound) {
		var discovered = compound.getList("pads", NbtElement.COMPOUND_TYPE);
		var modChunks = compound.getList("chunks", NbtElement.COMPOUND_TYPE);


		for (NbtElement el : discovered) {
			var cmp = (NbtCompound) el;

			var ow = NbtHelper.toBlockPos(cmp.getCompound("ow"));
			var sw = NbtHelper.toBlockPos(cmp.getCompound("sw"));

			var data = new TelepadData();
			data.overworldPosition = ow;
			data.spiritWorldPosition = sw;

			allDiscoveredPads.add(data);

			overworldMap.put(data.overworldPosition, data);
			spiritWorldMap.put(data.spiritWorldPosition, data);
		}

		for (NbtElement el : modChunks) {
			var cmp = (NbtCompound) el;

			modifiedChunkPositions.add(NbtHelper.toBlockPos(cmp));
		}
	}


	/**
	 * Called when an entity uses a teleporter pad in the overworld, teleporting to this dimension
	 *
	 * @param entity
	 * @param position
	 */
	public void useInTelepad(ServerPlayerEntity entity, BlockPos position) {
		//Verify teleporter pad exists in the overworld before adding it.
		if (!PadsMultiblocks.padStructure.verify(server.getOverworld(), position))
			throw new IllegalStateException("No teleporter pad found at " + position + " in the overworld");

		//Create a new teleporter pad data for this position in the overworld, if none exists.
		if (!overworldMap.containsKey(position)) {
			var data = new TelepadData();
			data.overworldPosition = position;

			allDiscoveredPads.add(data);
			overworldMap.put(position, data);
		}

		rebuildWorld();

		var pairedPad = overworldMap.get(position);

		entity.teleport(actualWorld, pairedPad.spiritWorldPosition.getX() + 0.5f, pairedPad.spiritWorldPosition.getY() + 2, pairedPad.spiritWorldPosition.getZ() + 0.5f, entity.getYaw(), entity.getPitch());
	}


	/**
	 * Called when an entity uses a teleporter pad in this dimension, to leave.
	 *
	 * @param entity
	 * @param position
	 */
	public void useOutTelepad(ServerPlayerEntity entity, BlockPos position) {
		var pairedPad = spiritWorldMap.get(position);
		entity.teleport(server.getOverworld(), pairedPad.overworldPosition.getX() + 0.5f, pairedPad.overworldPosition.getY() + 2, pairedPad.overworldPosition.getZ() + 0.5f, entity.getYaw(), entity.getPitch());
	}


	private void rebuildWorld() {
		//Clear out the existing world data...
		var biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);

		for (BlockPos pos : modifiedChunkPositions) {
			var chunk = actualWorld.getChunk(pos.getX(), pos.getZ());
			var sections = chunk.getSectionArray();

			for (int i = 0; i < sections.length; i++) {
				sections[i] = new ChunkSection(actualWorld.sectionIndexToCoord(i), biomeRegistry);
			}

			for (Map.Entry<Heightmap.Type, Heightmap> heightmap : chunk.getHeightmaps()) {
				var arr = heightmap.getValue().asLongArray();
				Arrays.fill(arr, 0);
			}
		}

		modifiedChunkPositions.clear();


		for (int i = allDiscoveredPads.size() - 1; i >= 0; i--) {
			var pad = allDiscoveredPads.get(i);

			if(PadsMultiblocks.padStructure.verify(server.getOverworld(), pad.overworldPosition))
				continue;

			allDiscoveredPads.remove(i);
			overworldMap.remove(pad.overworldPosition);
			spiritWorldMap.remove(pad.spiritWorldPosition);
		}

		var start = System.currentTimeMillis();

		SpiritWorldBuilder.buildWorld(server.getOverworld(), this);

		var end = System.currentTimeMillis();

		System.out.println("took " + (end - start) + "ms to build world");
	}

	public void recordBlockChange(BlockPos pos) {
		int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
		int chunkY = ChunkSectionPos.getSectionCoord(pos.getZ());
		var chunkPos = new BlockPos(chunkX, 0, chunkY);

		modifiedChunkPositions.add(chunkPos);
	}

	public boolean setBlockState(BlockPos pos, BlockState state) {

		if (actualWorld.getBlockState(pos) != Blocks.AIR.getDefaultState())
			return false;

		recordBlockChange(pos);
		actualWorld.setBlockState(pos, state);

		return true;
	}

	public class TelepadData {
		public BlockPos overworldPosition;
		public BlockPos spiritWorldPosition;
	}
}
