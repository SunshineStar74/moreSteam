package com.sunshinestar.moreSteam.particle;

import com.sunshinestar.moreSteam.MoreSteam;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MoreSteamParticles {

	public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
		DeferredRegister.create(Registries.PARTICLE_TYPE, MoreSteam.MOD_ID);

	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BOGEY_STEAM =
		PARTICLE_TYPES.register("bogey_steam", () -> new SimpleParticleType(false));

	private MoreSteamParticles() {}
}
