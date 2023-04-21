package com.cassunshine.pads.multiblock;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import oshi.util.tuples.Pair;

import java.util.function.Function;

public class MultiblockStructure {

	public ImmutableList<Pair<BlockPos, Function<BlockState, Boolean>>> blockVerifiers;


	public MultiblockStructure() {
	}

	public MultiblockStructure(ImmutableList<Pair<BlockPos, Function<BlockState, Boolean>>> verifiers) {
		blockVerifiers = verifiers;
	}


	public boolean verify(World world, BlockPos pivot) {
		for (Pair<BlockPos, Function<BlockState, Boolean>> pair : blockVerifiers) {
			var checkPosition = pivot.add(pair.getA());
			var state = world.getBlockState(checkPosition);
			if (!pair.getB().apply(state))
				return false;
		}
		return true;
	}
}
