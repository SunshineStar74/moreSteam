package com.sunshinestar.moreSteam.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.sunshinestar.moreSteam.MoreSteamBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create's connected textures only join a block to the same block. This lets a train casing join a
 * firebox as well, so the seam is gone from both sides.
 */
@Mixin(ConnectedTextureBehaviour.class)
public abstract class ConnectedTextureBehaviourMixin {

	@Shadow
	protected abstract boolean isBeingBlocked(BlockState state, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos,
		Direction face);

	@ModifyReturnValue(
		method = "connectsTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
		at = @At("RETURN")
	)
	private boolean moresteam$casingJoinsFirebox(boolean connects, BlockState state, BlockState other,
		BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
		if (connects || !AllBlocks.RAILWAY_CASING.has(state) || !other.is(MoreSteamBlocks.FIREBOX.get()))
			return connects;
		return !isBeingBlocked(state, reader, pos, otherPos, face);
	}
}
