package com.cassunshine.pads.mixin;

import com.cassunshine.pads.PadsMod;
import com.cassunshine.pads.accessors.IPadUser;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin extends PlayerEntity implements IPadUser {

	private int pads_padTicks = 0;


	public ServerPlayerEntityMixin(World world, BlockPos pos, float f, GameProfile gameProfile) {
		super(world, pos, f, gameProfile);
	}

	@Override
	@Shadow
	public boolean isSpectator() {
		return false;
	}

	@Override
	@Shadow
	public boolean isCreative() {
		return false;
	}


	@Override
	public int getPadTicks() {
		return pads_padTicks;
	}

	@Override
	public void setPadTicks(int amount) {
		pads_padTicks = amount;
	}


	@Inject(at = @At("RETURN"), method = "tick")
	public void tick(CallbackInfo inf) {
		PadsMod.evalPlayer((ServerPlayerEntity) (Object) this);
	}
}
