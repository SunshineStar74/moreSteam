package com.sunshinestar.moreSteam.mixin;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.entity.CarriageBogey;

import net.createmod.catnip.animation.LerpedFloat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the package-private bogey block and orientation that Create uses to place its particles. */
@Mixin(CarriageBogey.class)
public interface CarriageBogeyAccessor {

	@Accessor("type")
	AbstractBogeyBlock<?> moresteam$getType();

	@Accessor("isLeading")
	boolean moresteam$isLeading();

	@Accessor("yaw")
	LerpedFloat moresteam$getYaw();

	@Accessor("pitch")
	LerpedFloat moresteam$getPitch();
}
