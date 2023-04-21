package com.cassunshine.pads.block;

import com.cassunshine.pads.PadsMod;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.SignType;
import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings;
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings;

public class PadsBlocks {

	//Used for solid blocks.
	public static final Block FULL_SPIRIT_BLOCK = new GlassBlock(QuiltBlockSettings.of(Material.AIR).nonOpaque());
	public static final Block STAIR_SPIRIT_BLOCK = new StairsBlock(FULL_SPIRIT_BLOCK.getDefaultState(), AbstractBlock.Settings.copy(FULL_SPIRIT_BLOCK));
	public static final Block SLAB_SPIRIT_BLOCK = new SlabBlock(AbstractBlock.Settings.copy(FULL_SPIRIT_BLOCK));
	public static final Block FENCE_SPIRIT_BLOCK = new FenceBlock(AbstractBlock.Settings.copy(FULL_SPIRIT_BLOCK));
	public static final Block GATE_SPIRIT_BLOCK = new FenceGateBlock(AbstractBlock.Settings.copy(FULL_SPIRIT_BLOCK), SignType.OAK);
	public static final Block WALL_SPIRIT_BLOCK = new WallBlock(AbstractBlock.Settings.copy(FULL_SPIRIT_BLOCK));

	//Used for leaves and glass
	public static final Block GLASS_SPIRIT_BLOCK = new Block(QuiltBlockSettings.of(Material.AIR).nonOpaque());

	public static void initialize() {
		register(FULL_SPIRIT_BLOCK, new Identifier(PadsMod.MOD_ID, "spirit_block"));
		register(STAIR_SPIRIT_BLOCK, new Identifier(PadsMod.MOD_ID, "spirit_stairs"));
		register(SLAB_SPIRIT_BLOCK, new Identifier(PadsMod.MOD_ID, "spirit_slab"));
		register(FENCE_SPIRIT_BLOCK, new Identifier(PadsMod.MOD_ID, "spirit_fence"));
		register(GATE_SPIRIT_BLOCK, new Identifier(PadsMod.MOD_ID, "spirit_gate"));
		register(WALL_SPIRIT_BLOCK, new Identifier(PadsMod.MOD_ID, "spirit_wall"));
		register(GLASS_SPIRIT_BLOCK, new Identifier(PadsMod.MOD_ID, "spirit_glass"));
	}


	private static void register(Block b, Identifier identifier) {
		Registry.register(Registries.BLOCK, identifier, b);
		Registry.register(Registries.ITEM, identifier, new BlockItem(b, new QuiltItemSettings().rarity(Rarity.RARE)));
	}
}
