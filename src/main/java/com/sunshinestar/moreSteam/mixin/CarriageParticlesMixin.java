package com.sunshinestar.moreSteam.mixin;

import com.simibubi.create.content.trains.entity.CarriageParticles;
import com.sunshinestar.moreSteam.particle.MoreSteamParticles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Limits the replacement to Create's CarriageParticles#tick bogey-stop emission.
 * It deliberately does not alter the vanilla POOF particle type or factory.
 */
@Mixin(CarriageParticles.class)
public abstract class CarriageParticlesMixin {

	@Shadow int depressurise;

	@ModifyArg(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"),
		index = 0
	)
	private ParticleOptions moresteam$replaceStoppedBogeyPoof(ParticleOptions particle) {
		return this.depressurise > 0 && particle == ParticleTypes.POOF
			? MoreSteamParticles.BOGEY_STEAM.get()
			: particle;
	}
}
