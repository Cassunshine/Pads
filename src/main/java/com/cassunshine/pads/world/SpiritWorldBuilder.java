package com.cassunshine.pads.world;

import com.cassunshine.pads.block.PadsBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.StairsBlock;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.random.LegacySimpleRandom;
import net.minecraft.world.ChunkSectionCache;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseHelper;

import java.util.ArrayList;
import java.util.Optional;

public class SpiritWorldBuilder {

	/**
	 * Minimum distance between telepads in the spirit world.
	 */
	public static final float MIN_DISTANCE = 24;
	public static final float DOUBLE_DISTANCE = MIN_DISTANCE * 2;


	public static void buildWorld(World sourceWorld, SpiritWorld spiritWorld) {
		//Clear mapped out positions.
		spiritWorld.spiritMap.clear();

		//Step 1 - Sort points by distance & direction to first telepad.
		var list = new ArrayList<>(spiritWorld.overworldMap.values());
		list.sort((a, b) -> (int) (a.overworldPos.getSquaredDistance(spiritWorld.firstPad.overworldPos) - b.overworldPos.getSquaredDistance(spiritWorld.firstPad.overworldPos)));

		//Copy over positions from overworld to spirit world, centered on the first pad.
		for (SpiritWorld.TelepadData data : list) {
			data.spiritWorldPos = data.overworldPos.subtract(spiritWorld.firstPad.overworldPos);
			data.spiritCalcPos = data.spiritWorldPos.ofCenter();
		}


		var placedPoints = new ArrayList<SpiritWorld.TelepadData>();
		placedPoints.add(spiritWorld.firstPad);

		//Foreach pad that's been discovered, place them as close as possible (radially outward from the spawn)
		for (int i = 1; i < list.size(); i++) {
			var pad = list.get(i);

			var placementRay = new PlacementRay();
			placementRay.point = pad.spiritCalcPos.subtract(0, pad.spiritCalcPos.y, 0).normalize().multiply(999999).negate();
			placementRay.direction = placementRay.point.normalize().negate();

			var furthestDistance = 0d;
			var furthestPoint = placementRay.point;

			//Raycast against points that have been placed.
			for (SpiritWorld.TelepadData point : placedPoints) {
				var obstructor = new PlacementObstructor();
				obstructor.point = point.spiritCalcPos;

				var hitPoint = obstructor.hitPoint(placementRay);
				if (!hitPoint.isPresent() || hitPoint.get() < furthestDistance)
					continue;

				furthestPoint = placementRay.getPoint(hitPoint.get());
				furthestDistance = hitPoint.get();
			}

			pad.spiritCalcPos = furthestPoint;
			placedPoints.add(pad);
		}

		//Finalize each point.
		for (SpiritWorld.TelepadData pad : placedPoints) {
			pad.spiritWorldPos = BlockPos.create(pad.spiritCalcPos.x, pad.spiritCalcPos.y, pad.spiritCalcPos.z);

			spiritWorld.spiritMap.put(pad.spiritWorldPos, pad);
		}
	}

	public static void copyTelepad(World source, World destination, BlockPos srcPos, BlockPos dstPos) {

		var sampler = new PerlinNoiseSampler(new LegacySimpleRandom(0));

		var cache = new ChunkSectionCache(source);

		//Copy surrounding area first.
		for (int x = -16; x <= 16; x++) {
			for (int y = -3; y <= 10; y++) {
				for (int z = -16; z <= 16; z++) {
					var pos = srcPos.add(x, y, z);
					var state = cache.getBlockState(pos);

					var influence = (Math.sqrt((x * x) + (z * z)) - 8) / 8;
					var noiseVal = sampler.sample(x * 0.3d, y * 0.3d, z * 0.3d);

					if (noiseVal + influence > 0.5f)
						continue;

					if (state.isAir())
						continue;

					if (state.getBlock() instanceof StairsBlock) {
						//var stairState = PadsBlocks.STAIR_SPIRIT_BLOCK.getDefaultState();
						//destination.setBlockState(dstPos.add(x, y, z), stairState);
					} else if (state.isFullCube(source, pos)) {
						destination.setBlockState(dstPos.add(x, y, z), PadsBlocks.FULL_SPIRIT_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_LIGHTING_UPDATES | Block.SKIP_DROPS);
					}
				}
			}
		}

		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				var state = source.getBlockState(srcPos.add(x, 0, z));
				destination.setBlockState(dstPos.add(x, 0, z), state);
			}
		}


		cache.close();
	}


	private static class PlacementObstructor {
		public Vec3d point;

		public Optional<Double> hitPoint(PlacementRay ray) {
			point = point.subtract(0, point.y, 0);

			var u = point.subtract(ray.point);
			var u1 = ray.direction.multiply(u.dotProduct(ray.direction));
			var u2 = u.subtract(u1);
			var d = u2.length();

			//No intersection point.
			if (d > DOUBLE_DISTANCE)
				return Optional.empty();

			var m = Math.sqrt(DOUBLE_DISTANCE * DOUBLE_DISTANCE - d * d);

			if (d < DOUBLE_DISTANCE) {
				return Optional.of(u1.length() + m);
			} else {
				return Optional.of(u1.length() - m);
			}
		}
	}

	private static class PlacementRay {
		public Vec3d point;
		public Vec3d direction;


		public Vec3d getPoint(double dbl) {
			return point.add(direction.multiply(dbl));
		}
	}
}
