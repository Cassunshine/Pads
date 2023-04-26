package com.cassunshine.pads.world;

import com.cassunshine.pads.block.PadsBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.random.LegacySimpleRandom;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

public class SpiritWorldBuilder {

	/**
	 * Minimum distance between telepads in the spirit world.
	 */
	public static final float MIN_DISTANCE = 32;
	public static final float DOUBLE_DISTANCE = MIN_DISTANCE * 2;

	public static Random worldRandom = new Random();


	public static void buildWorld(World sourceWorld, SpiritWorld spiritWorld) {
		//Clear all the positions for chunks that have been modified
		spiritWorld.modifiedChunkPositions.clear();

		// -- SORT TELEPORTER PAD PLACEMENT --

		//Sort by distance from primary
		var sortedPlaces = new ArrayList<TeleporterObstructor>();
		for (SpiritWorld.TelepadData pad : spiritWorld.allDiscoveredPads)
			sortedPlaces.add(new TeleporterObstructor(pad, spiritWorld.allDiscoveredPads.get(0)));
		sortedPlaces.sort((a, b) -> (int) (a.point.squaredDistanceTo(sortedPlaces.get(0).point) - b.point.squaredDistanceTo(sortedPlaces.get(0).point)));

		//Make array for final placements of pads
		var finalPlaces = new ArrayList<TeleporterObstructor>();
		finalPlaces.add(sortedPlaces.get(0)); //Initial one is what everything places off of, and it's always placed at 0,0

		for (int i = 1; i < sortedPlaces.size(); i++) {
			var pad = sortedPlaces.get(i);

			var ray = new PlacementRay();
			ray.point = pad.point.normalize().multiply(999999);
			ray.end = new Vec3d(0, 0, 0);

			double closestDistance = Float.POSITIVE_INFINITY;
			Vec3d closestPoint = pad.point;

			//Raycast against all existing points.
			for (TeleporterObstructor obstructor : finalPlaces) {
				var hitPoint = obstructor.hitPoint(ray);

				if (hitPoint.isEmpty())
					continue;

				var rayPos = hitPoint.get();

				if (rayPos >= closestDistance)
					continue;

				closestDistance = rayPos;
				closestPoint = ray.getPoint(closestDistance);
				pad.linkedPad = obstructor;
			}

			pad.point = closestPoint;
			finalPlaces.add(pad);
		}

		// -- BUILD PADS -- //

		var spiritState = PadsBlocks.FULL_SPIRIT_BLOCK.getDefaultState();
		var perlinNoise = new PerlinNoiseSampler(new LegacySimpleRandom(worldRandom.nextInt()));
		var overworld = spiritWorld.server.getOverworld();

		for (TeleporterObstructor finalPlace : finalPlaces) {
			//Move pad to appropriate position...
			finalPlace.data.spiritWorldPosition = BlockPos.create(finalPlace.point.x, finalPlace.point.y, finalPlace.point.z);
			spiritWorld.spiritWorldMap.put(finalPlace.data.spiritWorldPosition, finalPlace.data);

			//Copy telepad to spirit world....
			for (int x = -2; x <= 2; x++)
				for (int z = -2; z <= 2; z++) {
					var newPosSrc = finalPlace.data.overworldPosition.add(x, 0, z);
					var newPosDest = finalPlace.data.spiritWorldPosition.add(x, 0, z);
					spiritWorld.setBlockState(newPosDest, spiritWorld.server.getOverworld().getBlockState(newPosSrc));
				}

			for (int x = -15; x <= 15; x++)
				for (int z = -15; z <= 15; z++) {
					var finalPos = finalPlace.data.spiritWorldPosition.add(x, -1, z);

					double score = (x * x + z * z) / 90d;
					score += perlinNoise.sample(finalPos.getX() * 0.5d, finalPos.getY() * 0.2d, finalPos.getZ() * 0.5d) * 0.5f;
					if (score < 1) {
						spiritWorld.setBlockState(finalPos, spiritState);

						var height = Math.max(1 - score, 0) * 15;

						for (int y = 0; y < height; y++) {
							var overworldPos = finalPlace.data.overworldPosition.add(x, y, z);
							var actualPos = finalPos.add(0, 1 + y, 0);

							if (!overworld.getBlockState(overworldPos).isFullCube(overworld, overworldPos))
								continue;

							spiritWorld.setBlockState(actualPos, spiritState);
						}
					}
				}
		}

		//Draw paths...

		BlockPos[] directions = new BlockPos[]{
			new BlockPos(-1, 0, 0),
			new BlockPos(1, 0, 0),
			new BlockPos(0, 0, -1),
			new BlockPos(0, 0, 1)
		};

		BlockPos[] sideDirections = new BlockPos[]{
			new BlockPos(0, 0, 1),
			new BlockPos(0, 0, 1),
			new BlockPos(1, 0, 0),
			new BlockPos(1, 0, 0),
		};

		for (int i = 1; i < finalPlaces.size(); i++) {
			var current = finalPlaces.get(i).data.spiritWorldPosition.add(0, -1, 0);
			var dst = finalPlaces.get(i).linkedPad.data.spiritWorldPosition.add(0, -1, 0);

			var count = 0;

			while (current != dst && count++ < 256) {
				var lineData = getLineData(current, dst);

				for (int p = 0; p < lineData.length; p++) {
					current = current.add(lineData.direction);

					for (int w = -6; w <= 6; w++) {
						var tmp = current.add(lineData.sideDirection.multiply(w)).add(0, 0, 0);

						for (int h = -3; h <= 0; h++) {
							var tmpAgain = tmp.add(0, h, 0);

							var score = Math.abs(w) / 4;
							score += perlinNoise.sample(tmpAgain.getX() * 0.2f, tmpAgain.getY() * 0.2f, tmpAgain.getZ() * 0.2f) * 0.1f;

							if (score < 1)
								if (!spiritWorld.setBlockState(tmpAgain, spiritState))
									break;
						}
					}
				}

				//Draw end of line...
				for (int x = -2; x < 2; x++)
					for (int z = -2; z < 2; z++)
						spiritWorld.setBlockState(current.add(x, 0, z), spiritState);
			}
		}
	}

	private static LineData getLineData(BlockPos start, BlockPos end) {
		var diff = end.subtract(start);

		boolean pickedSide;

		//Pick the only side left with progress, or random.
		if (diff.getX() == 0)
			pickedSide = false;
		else if (diff.getZ() == 0)
			pickedSide = true;
		else
			pickedSide = worldRandom.nextBoolean();


		var data = new LineData();
		data.direction = new BlockPos(
			pickedSide ? Integer.compare(diff.getX(), 0) : 0,
			0,
			!pickedSide ? Integer.compare(diff.getZ(), 0) : 0
		);

		data.sideDirection = new BlockPos(
			!pickedSide ? Integer.compare(diff.getX(), 0) : 0,
			0,
			pickedSide ? Integer.compare(diff.getZ(), 0) : 0
		);

		var length = Math.abs(pickedSide ? diff.getX() : diff.getZ());

		if (length < 16)
			data.length = worldRandom.nextInt(0, length);
		else
			data.length = worldRandom.nextInt(8, length);

		return data;
	}


	private static class LineData {
		BlockPos direction;
		BlockPos sideDirection;
		int length;
	}

	private static class TeleporterObstructor extends PlacementObstructor {
		public SpiritWorld.TelepadData data;

		public TeleporterObstructor linkedPad;

		public TeleporterObstructor(SpiritWorld.TelepadData data, SpiritWorld.TelepadData primary) {
			this.data = data;
			this.point = data.overworldPosition.ofCenter().subtract(primary.overworldPosition.ofCenter());
			this.point = this.point.subtract(0, this.point.y, 0);
		}
	}

	private static class PlacementObstructor {
		public Vec3d point = new Vec3d(0, 0, 0);

		public Optional<Double> hitPoint(PlacementRay ray) {
			var d = ray.end.subtract(ray.point);
			var f = ray.point.subtract(point);

			var a = d.dotProduct(d);
			var b = 2 * f.dotProduct(d);
			var c = f.dotProduct(f) - (DOUBLE_DISTANCE * DOUBLE_DISTANCE);

			var discriminant = b * b - 4 * a * c;

			if (discriminant < 0) {
				return Optional.empty();
			} else {
				discriminant = Math.sqrt(discriminant);

				var t1 = (-b - discriminant) / (2 * a);

				if (t1 >= 0 && t1 <= 1) {
					return Optional.of(t1);
				} else {
					return Optional.empty();
				}
			}
		}
	}

	private static class PlacementRay {
		public Vec3d point;
		public Vec3d end;

		public Vec3d getPoint(double dbl) {
			return point.add(end.subtract(point).multiply(dbl));
		}
	}
}
