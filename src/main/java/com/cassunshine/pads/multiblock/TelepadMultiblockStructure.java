package com.cassunshine.pads.multiblock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import oshi.util.tuples.Pair;

import java.util.function.Function;

public class TelepadMultiblockStructure extends MultiblockStructure {

	private final ImmutableSet<Block> padBlocks;

	public TelepadMultiblockStructure(boolean includeStone) {
		super();

		{
			var builder = new ImmutableSet.Builder<Block>()
				.add(Blocks.GOLD_BLOCK)
				.add(Blocks.RAW_GOLD_BLOCK);

			if (includeStone)
				builder = builder.add(Blocks.STONE);

			padBlocks = builder.build();
		}

		var builder = new ImmutableList.Builder<Pair<BlockPos, Function<BlockState, Boolean>>>();

		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {

				if (x == -2 || x == 2 || z == -2 || z == 2)
					builder.add(new Pair<>(new BlockPos(x, 0, z), (blockState) -> blockState.getBlock() instanceof SlabBlock || blockState.getBlock() instanceof StairsBlock));
				else
					builder.add(new Pair<>(new BlockPos(x, 0, z), (blockState) -> padBlocks.contains(blockState.getBlock())));
			}
		}
		blockVerifiers = builder.build();
	}
}
