package com.sunshinestar.moreSteam.particle;

import com.sunshinestar.moreSteam.MoreSteam;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MoreSteamParticles {

	public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
		DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MoreSteam.MOD_ID);

	public static final RegistryObject<SimpleParticleType> BOGEY_STEAM = PARTICLE_TYPES.register("bogey_steam",
		() -> new SimpleParticleType(false));

	private MoreSteamParticles() {}

	public static void register() {
		PARTICLE_TYPES.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
	}
}
