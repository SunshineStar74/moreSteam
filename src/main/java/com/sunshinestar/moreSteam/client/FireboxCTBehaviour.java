package com.sunshinestar.moreSteam.client;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.sunshinestar.moreSteam.MoreSteamBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Connected textures for the firebox: the same edge pieces as Create's train casing, joining up
 * with other fireboxes and with train casings. The furnace door is drawn on top as its own layer.
 */
public final class FireboxCTBehaviour extends HorizontalCTBehaviour {

	public FireboxCTBehaviour() {
		super(AllSpriteShifts.RAILWAY_CASING_SIDE, AllSpriteShifts.RAILWAY_CASING);
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		return !isBeingBlocked(state, reader, pos, otherPos, face) && joinsCasing(other);
	}

	/** Whether a block is part of the train casing family: a train casing or a firebox. */
	public static boolean joinsCasing(BlockState state) {
		return state.is(MoreSteamBlocks.FIREBOX.get()) || AllBlocks.RAILWAY_CASING.has(state);
	}
}
